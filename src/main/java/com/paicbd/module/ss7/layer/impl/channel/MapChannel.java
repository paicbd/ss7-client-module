package com.paicbd.module.ss7.layer.impl.channel;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.MessageProcessing;
import com.paicbd.module.ss7.layer.api.channel.IChannelHandler;
import com.paicbd.module.ss7.layer.api.network.ILayer;
import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import com.paicbd.module.utils.Constants;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.dto.MessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.restcomm.protocols.ss7.map.api.MAPDialog;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@RequiredArgsConstructor
public class MapChannel implements IChannelHandler {

    private final JedisCluster jedisCluster;
    private final Gateway currentGateway;
    private final CdrProcessor cdrProcessor;
    private final String redisMessageRetryQueue;
    private final String redisMessageList;
    private final ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap;

    private MessageProcessing messageProcessing;

    @Override
    public void channelInitialize(ILayer layerInterface) {
        MapLayer map = (MapLayer) layerInterface;
        messageProcessing = new MessageProcessing(map, this.jedisCluster, this.currentGateway, this.cdrProcessor,
                this.redisMessageRetryQueue, this.redisMessageList, errorCodeMappingConcurrentHashMap);
        log.debug("MapChannel initialized complete!");
    }

    //from the listener
    @Override
    public void receiveMessageFromListener(ChannelMessage channelMessage) {
        try {
            String messageType = (String) channelMessage.getParameter(Constants.MESSAGE_TYPE);
            Objects.requireNonNull(messageType, "message type is null");
            boolean isMessageToProcess = (messageType.endsWith("_Request") || messageType.endsWith("_Response"));
            if (isMessageToProcess) {
                log.debug("[MAP::MESSAGE<{}>] with data {}", messageType, channelMessage);
                messageProcessing.processMessage(channelMessage);

            } else {
                MAPDialog mapDialog = (MAPDialog) channelMessage.getParameter(Constants.DIALOG);
                if (mapDialog != null) {
                    log.debug("[MAP::SIGNAL<{}>] dialogId = {}, applicationContext = {}, message = {}",
                            messageType, mapDialog.getLocalDialogId(), mapDialog.getApplicationContext(), channelMessage);
                    if (Constants.ON_ERROR_COMPONENT.equals(messageType) || Constants.ON_INVOKE_TIMEOUT.equals(messageType)) {
                        messageProcessing.processError(channelMessage, messageType);
                        return;
                    }
                    log.warn("No handler for message {} received from listener {}", messageType, channelMessage);
                }
            }
        } catch (Exception ex) {
            log.error("Error occurred on receive message from listeners. Error: ", ex);
        }
    }

    @Override
    public void sendMessage(MessageEvent message) {
        messageProcessing.sendMessage(message);
    }

}
