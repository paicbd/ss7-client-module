package com.paicbd.module.ss7.layer.impl.channel;

import com.paicbd.module.ss7.MessageProcessing;
import com.paicbd.module.ss7.layer.api.channel.IChannelHandler;
import com.paicbd.module.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class MapChannel implements IChannelHandler {

    private final MessageProcessing messageProcessing;

    //from the listener
    @Override
    public void receiveMessageFromListener(ChannelMessage channelMessage) {
        String messageType = (String) channelMessage.getParameter(Constants.MESSAGE_TYPE);
        Objects.requireNonNull(messageType, "message type is null");
        boolean isMessageToProcess = (messageType.endsWith("_Request") || messageType.endsWith("_Response"));
        if (isMessageToProcess) {
            log.debug("[MAP::MESSAGE<{}>] with data {}", messageType, channelMessage);
            this.messageProcessing.processMessage(channelMessage);
        } else {
            log.debug("[MAP::SIGNAL<{}>] with data {}", messageType, channelMessage);
            switch (messageType) {
                case Constants.ON_INVOKE_TIMEOUT, Constants.ON_ERROR_COMPONENT:
                    this.messageProcessing.processError(channelMessage, messageType);
                    break;

                case Constants.ON_DIALOG_CLOSE:
                    this.messageProcessing.processOnDialogClose(channelMessage);
                    break;

                default:
                    log.warn("No handler for message {} received from listener {}", messageType, channelMessage);
                    break;
            }
        }
    }

}
