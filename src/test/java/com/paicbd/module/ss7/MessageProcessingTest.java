package com.paicbd.module.ss7;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.impl.GatewayUtil;
import com.paicbd.module.ss7.layer.impl.MessageUtil;
import com.paicbd.module.ss7.layer.impl.channel.ChannelMessage;
import com.paicbd.module.ss7.layer.impl.channel.MapChannel;
import com.paicbd.module.ss7.layer.impl.network.LayerFactory;
import com.paicbd.module.ss7.layer.impl.network.layers.M3uaLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.SccpLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.SctpLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.TcapLayer;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.Constants;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.MessagePart;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.map.api.MAPApplicationContextName;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.MtForwardShortMessageResponse;
import org.restcomm.protocols.ss7.map.api.service.sms.SendRoutingInfoForSMResponse;
import org.restcomm.protocols.ss7.map.api.smstpdu.ValidityPeriodFormat;
import org.restcomm.protocols.ss7.map.errors.MAPErrorMessageAbsentSubscriberSMImpl;
import org.restcomm.protocols.ss7.map.errors.MAPErrorMessageSystemFailureImpl;
import redis.clients.jedis.JedisCluster;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class MessageProcessingTest {

    Gateway ss7Gateway = GatewayUtil.getGateway(4080, 4090);

    @Mock
    AppProperties appProperties;

    @Mock
    JedisCluster jedisCluster;

    @InjectMocks
    MapChannel mapChannel;

    @InjectMocks
    ExtendedResource extendedResource;

    @Mock
    CdrProcessor cdrProcessor;

    @Mock
    ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap;


    String path;
    SctpLayer sctpLayer;
    M3uaLayer m3uaLayer;
    SccpLayer sccpLayer;
    TcapLayer tcapLayer;
    MapLayer mapLayer;
    MessageProcessing messageProcessing;

    @BeforeEach
    void setUp() throws IOException {
        when(appProperties.getConfigPath()).thenReturn("");
        path = extendedResource.createDirectory(ss7Gateway.getName());
        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, ss7Gateway, path);
        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, ss7Gateway, path, sctpLayer);
        sccpLayer = (SccpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCCP", Ss7Utils.LayerType.SCCP, ss7Gateway, path, m3uaLayer);
        tcapLayer = (TcapLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-TCAP", Ss7Utils.LayerType.TCAP, ss7Gateway, path, sccpLayer);
        mapLayer = (MapLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-MAP", Ss7Utils.LayerType.MAP, ss7Gateway, path, tcapLayer);
        mapChannel.channelInitialize(mapLayer);
        mapLayer.setChannelHandler(mapChannel);
        messageProcessing = new MessageProcessing(mapLayer, jedisCluster, ss7Gateway, cdrProcessor, "", "", errorCodeMappingConcurrentHashMap);
    }

    @AfterEach
    void tearDown() throws IOException {
        extendedResource.deleteDirectory(new File(path));
    }

    void startLayers() {
        sctpLayer.start();
        m3uaLayer.start();
        sccpLayer.start();
        tcapLayer.start();
        mapLayer.start();
    }

    void stopLayers() {
        mapLayer.stop();
        tcapLayer.stop();
        sccpLayer.stop();
        m3uaLayer.stop();
        sctpLayer.stop();
    }


    @Test
    void testSendMessage() {
        startLayers();
        MessageEvent messageEventSri = MessageUtil.getMessageEvent();
        messageEventSri.setImsi(null);
        assertDoesNotThrow(() -> messageProcessing.sendMessage(messageEventSri));

        MessageEvent messageEventMtMessage = MessageUtil.getMessageEvent();
        assertDoesNotThrow(() -> messageProcessing.sendMessage(messageEventMtMessage));

        MessageEvent messageEventMtDelivery = MessageUtil.getMessageEvent();
        messageEventMtDelivery.setDlr(true);
        assertDoesNotThrow(() -> messageProcessing.sendMessage(messageEventMtDelivery));
        stopLayers();
    }

    @Test
    void testProcessMessage() throws MAPException {
        startLayers();

        this.processMoForwardShortMessageRequest();
        this.processSendRoutingInfoForSMResponse();
        this.processMtForwardShortMessageResponse();


        AlertServiceCentreRequest alertServiceCentreRequest;
        alertServiceCentreRequest = MessageUtil.createAlertServiceCentreRequest(this.mapLayer);
        ChannelMessage channelMessageAlertService = Ss7Utils.getMessage(alertServiceCentreRequest.getMessageType().toString());
        channelMessageAlertService.setParameter(Constants.MESSAGE, alertServiceCentreRequest);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageAlertService));

        stopLayers();
    }

    private void processSendRoutingInfoForSMResponse() throws MAPException {
        SendRoutingInfoForSMResponse  sendRoutingInfoForSMResponse = MessageUtil.createSriMessage(this.mapLayer);

        ChannelMessage channelMessageSriNull = Ss7Utils.getMessage(sendRoutingInfoForSMResponse.getMessageType().toString());
        channelMessageSriNull.setParameter(Constants.MESSAGE, sendRoutingInfoForSMResponse);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageSriNull));

        var messageSri = MessageUtil.getMessageEvent();
        addDataToHashMap("messageSriConcurrentHashMap", sendRoutingInfoForSMResponse.getMAPDialog().getLocalDialogId(), messageSri);
        ChannelMessage channelMessageSri = Ss7Utils.getMessage(sendRoutingInfoForSMResponse.getMessageType().toString());
        channelMessageSri.setParameter(Constants.MESSAGE, sendRoutingInfoForSMResponse);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageSri));

        messageSri.setDropMapSri(true);
        addDataToHashMap("messageSriConcurrentHashMap", sendRoutingInfoForSMResponse.getMAPDialog().getLocalDialogId(), messageSri);
        ChannelMessage channelMessageSriDropMapSri = Ss7Utils.getMessage(sendRoutingInfoForSMResponse.getMessageType().toString());
        channelMessageSriDropMapSri.setParameter(Constants.MESSAGE, sendRoutingInfoForSMResponse);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageSriDropMapSri));

        messageSri.setDropMapSri(false);
        messageSri.setCheckSriResponse(true);
        addDataToHashMap("messageSriConcurrentHashMap", sendRoutingInfoForSMResponse.getMAPDialog().getLocalDialogId(), messageSri);
        ChannelMessage channelMessageCheckSriResponse = Ss7Utils.getMessage(sendRoutingInfoForSMResponse.getMessageType().toString());
        channelMessageCheckSriResponse.setParameter(Constants.MESSAGE, sendRoutingInfoForSMResponse);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageCheckSriResponse));

        messageSri.setDropMapSri(false);
        messageSri.setCheckSriResponse(false);
        messageSri.setNetworkIdToMapSri(13);
        addDataToHashMap("messageSriConcurrentHashMap", sendRoutingInfoForSMResponse.getMAPDialog().getLocalDialogId(), messageSri);
        ChannelMessage channelMessageNetworkIdToMapSri = Ss7Utils.getMessage(sendRoutingInfoForSMResponse.getMessageType().toString());
        channelMessageNetworkIdToMapSri.setParameter(Constants.MESSAGE, sendRoutingInfoForSMResponse);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageNetworkIdToMapSri));

        messageSri.setDropMapSri(false);
        messageSri.setCheckSriResponse(false);
        messageSri.setNetworkIdToMapSri(0);
        MessagePart messagePart = new MessagePart();
        messagePart.setMessageId(System.currentTimeMillis() + "-" + System.nanoTime());
        messagePart.setShortMessage("Hello Word");
        messagePart.setUdhJson("{\"message\":\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Lorem ipsum dolor si\",\"0x00\":[4,4,1]}");
        messagePart.setMsgReferenceNumber("400");
        messagePart.setTotalSegment(4);
        messagePart.setSegmentSequence(1);
        messageSri.setRegisteredDelivery(0);
        messageSri.setMessageParts(List.of(messagePart));
        addDataToHashMap("messageSriConcurrentHashMap", sendRoutingInfoForSMResponse.getMAPDialog().getLocalDialogId(), messageSri);
        ChannelMessage channelMessageMultiPart = Ss7Utils.getMessage(sendRoutingInfoForSMResponse.getMessageType().toString());
        channelMessageMultiPart.setParameter(Constants.MESSAGE, sendRoutingInfoForSMResponse);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageMultiPart));
    }

    private void processMoForwardShortMessageRequest() throws MAPException {
        MoForwardShortMessageRequest moForwardShortMessageRequestImpl;
        moForwardShortMessageRequestImpl = MessageUtil.createMoMessage(ValidityPeriodFormat.fieldPresentRelativeFormat, this.mapLayer, 0);
        ChannelMessage channelMessageMoDataCoding0 = Ss7Utils.getMessage(moForwardShortMessageRequestImpl.getMessageType().toString());
        channelMessageMoDataCoding0.setParameter(Constants.MESSAGE, moForwardShortMessageRequestImpl);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageMoDataCoding0));

        moForwardShortMessageRequestImpl = MessageUtil.createMoMessage(ValidityPeriodFormat.fieldPresentRelativeFormat, this.mapLayer, 5);
        ChannelMessage channelMessageMoDataCoding15 = Ss7Utils.getMessage(moForwardShortMessageRequestImpl.getMessageType().toString());
        channelMessageMoDataCoding15.setParameter(Constants.MESSAGE, moForwardShortMessageRequestImpl);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageMoDataCoding15));
    }

    private void processMtForwardShortMessageResponse() throws MAPException {
        MtForwardShortMessageResponse mtForwardShortMessageResponse = MessageUtil.createMtResponse(this.mapLayer);

        ChannelMessage channelMessageMtResponseNull = Ss7Utils.getMessage(mtForwardShortMessageResponse.getMessageType().toString());
        channelMessageMtResponseNull.setParameter(Constants.MESSAGE, mtForwardShortMessageResponse);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageMtResponseNull));

        var message = MessageUtil.getMessageEvent();
        message.setDlr(false);
        addDataToHashMap("messageMtConcurrentHashMap", mtForwardShortMessageResponse.getMAPDialog().getLocalDialogId(), message);
        ChannelMessage channelMessageMtResponse = Ss7Utils.getMessage(mtForwardShortMessageResponse.getMessageType().toString());
        channelMessageMtResponse.setParameter(Constants.MESSAGE, mtForwardShortMessageResponse);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageMtResponse));

        message.setDlr(true);
        message.setRegisteredDelivery(0);
        addDataToHashMap("messageMtConcurrentHashMap", mtForwardShortMessageResponse.getMAPDialog().getLocalDialogId(), message);
        ChannelMessage channelMessageMtResponseDrl = Ss7Utils.getMessage(mtForwardShortMessageResponse.getMessageType().toString());
        channelMessageMtResponseDrl.setParameter(Constants.MESSAGE, mtForwardShortMessageResponse);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageMtResponseDrl));
        
        message.setDlr(false);
        message.setRegisteredDelivery(0);
        message.setOriginNetworkType("GW");
        message.setOriginProtocol("SMPP");
        message.setOriginNetworkId(10);
        addDataToHashMap("messageMtConcurrentHashMap", mtForwardShortMessageResponse.getMAPDialog().getLocalDialogId(), message);
        ChannelMessage channelMessageMtResponseGwOrigin = Ss7Utils.getMessage(mtForwardShortMessageResponse.getMessageType().toString());
        channelMessageMtResponseGwOrigin.setParameter(Constants.MESSAGE, mtForwardShortMessageResponse);
        assertDoesNotThrow(() -> this.messageProcessing.processMessage(channelMessageMtResponseGwOrigin));
    }



    @Test
    void testProcessError() throws MAPException {
        startLayers();
        this.processErrorComponent();
        this.processInvokeTimeout();
        stopLayers();
    }

    private void processErrorComponent() throws MAPException {
        ChannelMessage channelMessageErrorComponentNull = Ss7Utils.getMessage(Constants.ON_ERROR_COMPONENT);
        assertDoesNotThrow(() -> this.messageProcessing.processError(channelMessageErrorComponentNull, Constants.ON_ERROR_COMPONENT));

        ChannelMessage channelMessageErrorComponent = Ss7Utils.getMessage(Constants.ON_ERROR_COMPONENT);
        var mapDialog = MessageUtil.createMapDialog(this.mapLayer, MAPApplicationContextName.shortMsgMTRelayContext);
        channelMessageErrorComponent.setParameter(Constants.DIALOG, mapDialog);
        channelMessageErrorComponent.setParameter(Constants.INVOKE_ID, 100);
        channelMessageErrorComponent.setParameter(Constants.MAP_ERROR_MESSAGE, new MAPErrorMessageSystemFailureImpl());
        //Empty Message
        assertDoesNotThrow(() -> this.messageProcessing.processError(channelMessageErrorComponent, Constants.ON_ERROR_COMPONENT));

        //Adding Message dummy
        var messageEvent = MessageUtil.getMessageEvent();
        addDataToHashMap("messageMtConcurrentHashMap", mapDialog.getLocalDialogId(), messageEvent);
        assertDoesNotThrow(() -> this.messageProcessing.processError(channelMessageErrorComponent, Constants.ON_ERROR_COMPONENT));

        messageEvent.setNetworkIdToPermanentFailure(10);
        addDataToHashMap("messageMtConcurrentHashMap", mapDialog.getLocalDialogId(), messageEvent);
        assertDoesNotThrow(() -> this.messageProcessing.processError(channelMessageErrorComponent, Constants.ON_ERROR_COMPONENT));

        ChannelMessage channelMessageErrorComponentTempError = Ss7Utils.getMessage(Constants.ON_ERROR_COMPONENT);
        var mapDialogTempError = MessageUtil.createMapDialog(this.mapLayer, MAPApplicationContextName.shortMsgMTRelayContext);
        messageEvent.setLastRetry(true);
        channelMessageErrorComponentTempError.setParameter(Constants.DIALOG, mapDialogTempError);
        channelMessageErrorComponentTempError.setParameter(Constants.INVOKE_ID, 100);
        channelMessageErrorComponentTempError.setParameter(Constants.MAP_ERROR_MESSAGE, new MAPErrorMessageAbsentSubscriberSMImpl());
        addDataToHashMap("messageMtConcurrentHashMap", mapDialogTempError.getLocalDialogId(), messageEvent);
        assertDoesNotThrow(() -> this.messageProcessing.processError(channelMessageErrorComponentTempError, Constants.ON_ERROR_COMPONENT));

        messageEvent.setNetworkIdTempFailure(20);
        addDataToHashMap("messageMtConcurrentHashMap", mapDialogTempError.getLocalDialogId(), messageEvent);
        assertDoesNotThrow(() -> this.messageProcessing.processError(channelMessageErrorComponentTempError, Constants.ON_ERROR_COMPONENT));
    }

    private void processInvokeTimeout() throws MAPException {
        var mapDialog = MessageUtil.createMapDialog(this.mapLayer, MAPApplicationContextName.shortMsgMTRelayContext);
        ChannelMessage channelMessageInvokeTimeoutMessageNull = Ss7Utils.getMessage(Constants.ON_INVOKE_TIMEOUT);
        channelMessageInvokeTimeoutMessageNull.setParameter(Constants.DIALOG, mapDialog);
        channelMessageInvokeTimeoutMessageNull.setParameter(Constants.INVOKE_ID, 100);
        channelMessageInvokeTimeoutMessageNull.setParameter(Constants.MAP_ERROR_MESSAGE, new MAPErrorMessageSystemFailureImpl());
        assertDoesNotThrow(() -> this.messageProcessing.processError(channelMessageInvokeTimeoutMessageNull, Constants.ON_INVOKE_TIMEOUT));

        ChannelMessage channelMessageInvokeTimeout = Ss7Utils.getMessage(Constants.ON_INVOKE_TIMEOUT);
        channelMessageInvokeTimeout.setParameter(Constants.DIALOG, mapDialog);
        channelMessageInvokeTimeout.setParameter(Constants.INVOKE_ID, 100);
        channelMessageInvokeTimeout.setParameter(Constants.MAP_ERROR_MESSAGE, new MAPErrorMessageSystemFailureImpl());
        var messageEvent = MessageUtil.getMessageEvent();
        addDataToHashMap("messageMtConcurrentHashMap", mapDialog.getLocalDialogId(), messageEvent);
        assertDoesNotThrow(() -> this.messageProcessing.processError(channelMessageInvokeTimeout, Constants.ON_INVOKE_TIMEOUT));
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<Long, MessageEvent> getConcurrentHashMap(String hashMap) throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = messageProcessing.getClass();
        var field = clazz.getDeclaredField(hashMap);
        field.setAccessible(true);
        return (ConcurrentHashMap<Long, MessageEvent>) field.get(messageProcessing);
    }

    private void addDataToHashMap(String hashMap, long dialogId, MessageEvent messageEvent)  {
        try {
            getConcurrentHashMap(hashMap).put(dialogId, messageEvent);
        } catch (Exception e) {
            log.error("Error on put data on {}", hashMap);
        }
    }
}