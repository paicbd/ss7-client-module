package com.paicbd.module.ss7;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.impl.GatewayUtil;
import com.paicbd.module.ss7.layer.impl.MessageUtil;
import com.paicbd.module.ss7.layer.impl.channel.MapChannel;
import com.paicbd.module.ss7.layer.impl.network.LayerFactory;
import com.paicbd.module.ss7.layer.impl.network.layers.M3uaLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.SccpLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.SctpLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.TcapLayer;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.dto.MessageEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.ValidityPeriodFormat;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageFactoryTest {

    Gateway ss7Gateway = GatewayUtil.getGateway(2080, 2090);

    @Mock
    AppProperties appProperties;

    @InjectMocks
    MapChannel mapChannel;

    @InjectMocks
    ExtendedResource extendedResource;


    String path;
    SctpLayer sctpLayer;
    M3uaLayer m3uaLayer;
    SccpLayer sccpLayer;
    TcapLayer tcapLayer;
    MapLayer mapLayer;
    MessageFactory messageFactory;

    @BeforeEach
    void setUp() throws IOException {
        when(appProperties.getConfigPath()).thenReturn("");
        path = extendedResource.createDirectory(ss7Gateway.getName());
        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, ss7Gateway, path);
        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, ss7Gateway, path, sctpLayer);
        sccpLayer = (SccpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCCP", Ss7Utils.LayerType.SCCP, ss7Gateway, path, m3uaLayer);
        tcapLayer = (TcapLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-TCAP", Ss7Utils.LayerType.TCAP, ss7Gateway, path, sccpLayer);
        mapLayer = (MapLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-MAP", Ss7Utils.LayerType.MAP, ss7Gateway, path, tcapLayer);
        messageFactory = new MessageFactory(mapLayer);
        mapChannel.channelInitialize(mapLayer);
        mapLayer.setChannelHandler(mapChannel);
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
    void testCreateSendRoutingInfoForSMRequestFromMessageEvent() throws MAPException {
        startLayers();
        MessageEvent messageEvent = MessageUtil.getMessageEvent();
        var sriMessage = this.messageFactory.createSendRoutingInfoForSMRequestFromMessageEvent(messageEvent);
        assertNotNull(sriMessage);
        assertEquals(3, sriMessage.getApplicationContext().getApplicationContextVersion().getVersion());
        var remoteAddress = sriMessage.getRemoteAddress();
        var localAddress = sriMessage.getLocalAddress();
        assertNotNull(remoteAddress);
        assertEquals("22222222", remoteAddress.getGlobalTitle().getDigits());
        assertEquals(0, remoteAddress.getSignalingPointCode());
        assertEquals(6, remoteAddress.getSubsystemNumber());
        assertNotNull(localAddress);
        assertEquals("888888", localAddress.getGlobalTitle().getDigits());
        assertEquals(0, localAddress.getSignalingPointCode());
        assertEquals(8, localAddress.getSubsystemNumber());
        stopLayers();
    }

    void checkCorrectMtForwardSMRequestValues(MessageEvent messageEvent, MAPDialogSms mapDialogSms) {
        assertNotNull(mapDialogSms);
        assertEquals(3, mapDialogSms.getApplicationContext().getApplicationContextVersion().getVersion());
        var remoteAddress = mapDialogSms.getRemoteAddress();
        var localAddress = mapDialogSms.getLocalAddress();
        assertNotNull(remoteAddress);
        assertEquals(messageEvent.getNetworkNodeNumber(), remoteAddress.getGlobalTitle().getDigits());
        assertEquals(0, remoteAddress.getSignalingPointCode());
        assertEquals(messageEvent.getMscSsn(), remoteAddress.getSubsystemNumber());

        assertEquals(messageEvent.getSccpCalledPartyAddress(), remoteAddress.getGlobalTitle().getDigits());
        assertEquals(messageEvent.getSccpCalledPartyAddressPointCode(), remoteAddress.getSignalingPointCode());
        assertEquals(messageEvent.getSccpCalledPartyAddressSubSystemNumber(), remoteAddress.getSubsystemNumber());

        assertNotNull(localAddress);
        assertEquals(messageEvent.getGlobalTitle(), localAddress.getGlobalTitle().getDigits());
        assertEquals(0, localAddress.getSignalingPointCode());
        assertEquals(messageEvent.getSmscSsn(), localAddress.getSubsystemNumber());

        assertEquals(messageEvent.getSccpCallingPartyAddress(), localAddress.getGlobalTitle().getDigits());
        assertEquals(messageEvent.getSccpCallingPartyAddressPointCode(), localAddress.getSignalingPointCode());
        assertEquals(messageEvent.getSccpCallingPartyAddressSubSystemNumber(), localAddress.getSubsystemNumber());
    }

    @Test
    void testCreateMtForwardSMRequestFromMessageEvent() throws MAPException {
        startLayers();
        MessageEvent messageEvent;
        MAPDialogSms mtMessage;

        messageEvent = MessageUtil.getMessageEvent();
        mtMessage = this.messageFactory.createMtForwardSMRequestFromMessageEvent(messageEvent);
        checkCorrectMtForwardSMRequestValues(messageEvent, mtMessage);

        messageEvent.setDataCoding(8);
        mtMessage = this.messageFactory.createMtForwardSMRequestFromMessageEvent(messageEvent);
        checkCorrectMtForwardSMRequestValues(messageEvent, mtMessage);

        messageEvent.setDlr(true);
        mtMessage = this.messageFactory.createMtForwardSMRequestFromMessageEvent(messageEvent);
        checkCorrectMtForwardSMRequestValues(messageEvent, mtMessage);

        messageEvent.setUdhJson("{\"message\":\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Lorem ipsum dolor si\",\"0x00\":[4,4,1]}");
        messageEvent.setMsgReferenceNumber("4");
        messageEvent.setTotalSegment(4);
        messageEvent.setSegmentSequence(1);
        messageEvent.setRegisteredDelivery(0);
        mtMessage = this.messageFactory.createMtForwardSMRequestFromMessageEvent(messageEvent);
        checkCorrectMtForwardSMRequestValues(messageEvent, mtMessage);

        messageEvent.setUdhJson("{\"message\":\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Lorem ipsum dolor si\",\"0x00\":[4,4,1]}");
        messageEvent.setMsgReferenceNumber("400");
        messageEvent.setTotalSegment(4);
        messageEvent.setSegmentSequence(1);
        messageEvent.setRegisteredDelivery(0);
        mtMessage = this.messageFactory.createMtForwardSMRequestFromMessageEvent(messageEvent);
        checkCorrectMtForwardSMRequestValues(messageEvent, mtMessage);

        stopLayers();
    }


    @Test
    void testCreateDeliveryReceiptMessage() {
        MessageEvent messageEvent = MessageUtil.getMessageEvent();
        var deliverMessageEvent = this.messageFactory.createDeliveryReceiptMessage(messageEvent, null, null);
        checkCorrectDeliveryReceiptMessage(messageEvent, deliverMessageEvent);

        ErrorCodeMapping defaultMapping = new ErrorCodeMapping();
        defaultMapping.setErrorCode(0);
        defaultMapping.setDeliveryErrorCode(0);
        defaultMapping.setDeliveryStatus("UNDELIV");
        var deliverMessageEventWithErrorCode = this.messageFactory.createDeliveryReceiptMessage(messageEvent, defaultMapping, null);
        checkCorrectDeliveryReceiptMessage(messageEvent, deliverMessageEventWithErrorCode);

        String extraString = " imsi:" +
                messageEvent.getImsi() +
                " nnn_digits:" +
                messageEvent.getNetworkNodeNumber() +
                " nnn_an:" +
                messageEvent.getNetworkNodeNumberNatureOfAddress() +
                " nnn_np:" +
                messageEvent.getNetworkNodeNumberNumberingPlan();
        var deliverMessageEventWithExtraInformation = this.messageFactory.createDeliveryReceiptMessage(messageEvent, null, extraString);
        checkCorrectDeliveryReceiptMessage(messageEvent, deliverMessageEventWithExtraInformation);
    }

    void checkCorrectDeliveryReceiptMessage(MessageEvent messageEvent, MessageEvent deliverMessageEvent) {
        assertNotNull(deliverMessageEvent);
        assertFalse(deliverMessageEvent.getCheckSubmitSmResponse());
        assertTrue(deliverMessageEvent.isDlr());
        assertEquals(messageEvent.getOriginNetworkId(), deliverMessageEvent.getDestNetworkId());
        assertEquals(messageEvent.getOriginNetworkType(), deliverMessageEvent.getDestNetworkType());
        assertEquals(messageEvent.getOriginProtocol(), deliverMessageEvent.getDestProtocol());
        assertEquals(messageEvent.getDestNetworkId(), deliverMessageEvent.getOriginNetworkId());
        assertEquals(messageEvent.getDestNetworkType(), deliverMessageEvent.getOriginNetworkType());
        assertEquals(messageEvent.getDestProtocol(), deliverMessageEvent.getOriginProtocol());
        assertEquals(messageEvent.getSourceAddr(), deliverMessageEvent.getDestinationAddr());
        assertEquals(messageEvent.getSourceAddrTon(), deliverMessageEvent.getDestAddrTon());
        assertEquals(messageEvent.getSourceAddrNpi(), deliverMessageEvent.getDestAddrNpi());
        assertEquals(messageEvent.getDestinationAddr(), deliverMessageEvent.getSourceAddr());
        assertEquals(messageEvent.getDestAddrTon(), deliverMessageEvent.getSourceAddrTon());
        assertEquals(messageEvent.getDestAddrNpi(), deliverMessageEvent.getSourceAddrNpi());

        assertEquals(deliverMessageEvent.getDestinationAddr(), deliverMessageEvent.getMsisdn());
        assertEquals(deliverMessageEvent.getDestAddrTon(), deliverMessageEvent.getAddressNatureMsisdn());
        assertEquals(deliverMessageEvent.getDestAddrNpi(), deliverMessageEvent.getNumberingPlanMsisdn());
    }



    @Test
    void testCreateMessageEventFromMoForwardShortMessageRequest() throws MAPException {
        startLayers();
        MoForwardShortMessageRequest moForwardShortMessageRequestImpl;
        MessageEvent messageEventMo;
        SmsSignalInfo smsSignalInfo;
        SmsTpdu smsTpdu;
        SmsSubmitTpdu smsSubmitTpdu;

        moForwardShortMessageRequestImpl = MessageUtil.createMoMessage(ValidityPeriodFormat.fieldPresentRelativeFormat, this.mapLayer, 0);
        smsSignalInfo = moForwardShortMessageRequestImpl.getSM_RP_UI();
        smsTpdu = smsSignalInfo.decodeTpdu(true);
        smsSubmitTpdu = (SmsSubmitTpdu) smsTpdu;
        messageEventMo = this.messageFactory.createMessageEventFromMoForwardShortMessageRequest(moForwardShortMessageRequestImpl, smsSubmitTpdu);
        assertNotNull(messageEventMo);

        moForwardShortMessageRequestImpl = MessageUtil.createMoMessage(ValidityPeriodFormat.fieldPresentAbsoluteFormat, this.mapLayer, 0);
        smsSignalInfo = moForwardShortMessageRequestImpl.getSM_RP_UI();
        smsTpdu = smsSignalInfo.decodeTpdu(true);
        smsSubmitTpdu = (SmsSubmitTpdu) smsTpdu;
        messageEventMo = this.messageFactory.createMessageEventFromMoForwardShortMessageRequest(moForwardShortMessageRequestImpl, smsSubmitTpdu);
        assertNotNull(messageEventMo);

        moForwardShortMessageRequestImpl = MessageUtil.createMoMessage(ValidityPeriodFormat.fieldPresentEnhancedFormat, this.mapLayer, 0);
        smsSignalInfo = moForwardShortMessageRequestImpl.getSM_RP_UI();
        smsTpdu = smsSignalInfo.decodeTpdu(true);
        smsSubmitTpdu = (SmsSubmitTpdu) smsTpdu;
        messageEventMo = this.messageFactory.createMessageEventFromMoForwardShortMessageRequest(moForwardShortMessageRequestImpl, smsSubmitTpdu);
        assertNotNull(messageEventMo);

        stopLayers();
    }

    @Test
    void testCreateReportSMDeliveryStatusRequestFromMessageEvent() throws MAPException {
        startLayers();
        MAPDialogSms mapDialogSms;
        MessageEvent messageEvent = MessageUtil.getMessageEvent();

        mapDialogSms = this.messageFactory.createReportSMDeliveryStatusRequestFromMessageEvent(messageEvent, true);
        assertNotNull(mapDialogSms);

        mapDialogSms = this.messageFactory.createReportSMDeliveryStatusRequestFromMessageEvent(messageEvent, false);
        assertNotNull(mapDialogSms);

        stopLayers();
    }



}