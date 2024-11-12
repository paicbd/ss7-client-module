package com.paicbd.module.ss7;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.utils.MessageUtil;
import com.paicbd.module.ss7.layer.impl.channel.MapChannel;
import com.paicbd.module.ss7.layer.impl.network.LayerFactory;
import com.paicbd.module.ss7.layer.impl.network.layers.M3uaLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.SccpLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.SctpLayer;
import com.paicbd.module.ss7.layer.impl.network.layers.TcapLayer;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.CustomNumberingPlanIndicator;
import com.paicbd.module.utils.CustomTypeOfNumber;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.module.utils.GatewayCreator;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.dto.MessageEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.restcomm.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.restcomm.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.api.service.sms.SmsSignalInfo;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.ValidityPeriodFormat;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageFactoryTest {

    @Mock
    AppProperties appProperties;

    @Mock
    MapChannel mapChannel;

    ExtendedResource extendedResource;

    String path;

    SctpLayer sctpLayer;

    M3uaLayer m3uaLayer;

    SccpLayer sccpLayer;

    TcapLayer tcapLayer;

    MapLayer mapLayer;

    MessageFactory messageFactory;

    @BeforeEach
    void setUp() {
        Gateway ss7Gateway = GatewayCreator.buildSS7Gateway("ss7gwSCTP", 1, 4);
        when(appProperties.getConfigPath()).thenReturn("");
        extendedResource = new ExtendedResource(appProperties);
        path = extendedResource.createDirectory(ss7Gateway.getName());
        sctpLayer = (SctpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCTP", Ss7Utils.LayerType.SCTP, ss7Gateway, path);
        m3uaLayer = (M3uaLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-M3UA", Ss7Utils.LayerType.M3UA, ss7Gateway, path, sctpLayer);
        sccpLayer = (SccpLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-SCCP", Ss7Utils.LayerType.SCCP, ss7Gateway, path, m3uaLayer);
        tcapLayer = (TcapLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-TCAP", Ss7Utils.LayerType.TCAP, ss7Gateway, path, sccpLayer);
        mapLayer = (MapLayer) LayerFactory.createLayerInstance(ss7Gateway.getName() + "-MAP", Ss7Utils.LayerType.MAP, ss7Gateway, path, tcapLayer);
        messageFactory = new MessageFactory(mapLayer);
        mapLayer.setChannelHandler(mapChannel);
    }

    @AfterEach
    void tearDown() {
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
    @DisplayName("Create SRIForSM Request From Message Event When Has Message Event Then It Creates Map Dialog Sms")
    void createSendRoutingInfoForSMRequestFromMessageEventWhenHasMessageEventThenItCreatesMapDialogSms() throws MAPException {
        this.startLayers();
        MessageEvent messageEvent = MessageEvent.builder()
                .id("1722446896082-12194920127675")
                .messageId("1722446896081-12194920043917")
                .systemId("smpp_sp")
                .sourceAddr("8888")
                .destinationAddr("1234")
                .shortMessage("Hello!")
                .originNetworkId(3)
                .originNetworkType("SP")
                .originProtocol("SMPP")
                .destNetworkType("GW")
                .destProtocol("SS7")
                .destNetworkId(5)
                .sourceAddrTon(1)
                .sourceAddrNpi(4)
                .destAddrTon(1)
                .destAddrNpi(4)
                .routingId(1)
                .translationType(0)
                .globalTitle("2222")
                .globalTitleIndicator("GT0100")
                .msisdn("1234")
                .mscSsn(8)
                .hlrSsn(6)
                .smscSsn(8)
                .validityPeriod(120)
                .mapVersion(3)
                .networkIdToMapSri(-1)
                .networkIdToPermanentFailure(-1)
                .networkIdTempFailure(-1)
                .build();
        var sriMessage = this.messageFactory.createSendRoutingInfoForSMRequestFromMessageEvent(messageEvent);
        assertNotNull(sriMessage);
        assertEquals(messageEvent.getMapVersion(), sriMessage.getApplicationContext().getApplicationContextVersion().getVersion());
        var remoteAddress = sriMessage.getRemoteAddress();
        var localAddress = sriMessage.getLocalAddress();
        assertNotNull(remoteAddress);
        assertEquals(messageEvent.getMsisdn(), remoteAddress.getGlobalTitle().getDigits());
        assertEquals(0, remoteAddress.getSignalingPointCode());
        assertEquals(messageEvent.getHlrSsn(), remoteAddress.getSubsystemNumber());
        assertNotNull(localAddress);
        assertEquals(messageEvent.getGlobalTitle(), localAddress.getGlobalTitle().getDigits());
        assertEquals(0, localAddress.getSignalingPointCode());
        assertEquals(messageEvent.getSmscSsn(), localAddress.getSubsystemNumber());
        this.stopLayers();
    }

    @ParameterizedTest
    @MethodSource("messageEventToTestMtForwardSMRequest")
    @DisplayName("Create MTForwardSM Request From Message Event When Has Message Event Then It Creates Map Dialog Sms")
    void createMtForwardSMRequestFromMessageEventWhenHasMessageEventThenItCreatesMapDialogSms(MessageEvent messageEvent) throws MAPException {
        startLayers();
        MAPDialogSms mtMessage = this.messageFactory.createMtForwardSMRequestFromMessageEvent(messageEvent);
        checkCorrectMtForwardSMRequestValues(messageEvent, mtMessage);
        stopLayers();
    }

    static Stream<MessageEvent> messageEventToTestMtForwardSMRequest() {
        return Stream.of(
                MessageEvent.builder()
                        .id("1722446896082-12194920127675")
                        .messageId("1722446896081-12194920043917")
                        .systemId("smpp_sp")
                        .sourceAddr("8888")
                        .destinationAddr("1234")
                        .shortMessage("Hello!")
                        .originNetworkId(3)
                        .originNetworkType("SP")
                        .originProtocol("SMPP")
                        .destNetworkType("GW")
                        .destProtocol("SS7")
                        .destNetworkId(5)
                        .sourceAddrTon(1)
                        .sourceAddrNpi(4)
                        .destAddrTon(1)
                        .destAddrNpi(4)
                        .routingId(1)
                        .translationType(0)
                        .globalTitle("2222")
                        .globalTitleIndicator("GT0100")
                        .msisdn("1234")
                        .mscSsn(8)
                        .hlrSsn(6)
                        .smscSsn(8)
                        .dataCoding(0)
                        .imsi("748031234567890")
                        .networkNodeNumber("598991900032")
                        .networkNodeNumberNatureOfAddress(4)
                        .networkNodeNumberNumberingPlan(1)
                        .validityPeriod(120)
                        .mapVersion(3)
                        .networkIdToMapSri(-1)
                        .networkIdToPermanentFailure(-1)
                        .networkIdTempFailure(-1)
                        .build(),
                //DataCoding = 8
                MessageEvent.builder()
                        .id("1722446896082-12194920127675")
                        .messageId("1722446896081-12194920043917")
                        .systemId("smpp_sp")
                        .sourceAddr("8888")
                        .destinationAddr("1234")
                        .shortMessage("Hello!")
                        .originNetworkId(3)
                        .originNetworkType("SP")
                        .originProtocol("SMPP")
                        .destNetworkType("GW")
                        .destProtocol("SS7")
                        .destNetworkId(5)
                        .sourceAddrTon(1)
                        .sourceAddrNpi(4)
                        .destAddrTon(1)
                        .destAddrNpi(4)
                        .routingId(1)
                        .translationType(0)
                        .globalTitle("2222")
                        .globalTitleIndicator("GT0100")
                        .msisdn("1234")
                        .mscSsn(8)
                        .hlrSsn(6)
                        .smscSsn(8)
                        .dataCoding(8)
                        .imsi("748031234567890")
                        .networkNodeNumber("598991900032")
                        .networkNodeNumberNatureOfAddress(4)
                        .networkNodeNumberNumberingPlan(1)
                        .validityPeriod(120)
                        .mapVersion(3)
                        .networkIdToMapSri(-1)
                        .networkIdToPermanentFailure(-1)
                        .networkIdTempFailure(-1)
                        .build(),
                //Message DLR
                MessageEvent.builder()
                        .id("1722446896082-12194920127675")
                        .messageId("1722446896081-12194920043917")
                        .deliverSmId("1")
                        .systemId("smpp_sp")
                        .sourceAddr("8888")
                        .destinationAddr("1234")
                        .shortMessage("Hello!")
                        .originNetworkId(3)
                        .originNetworkType("SP")
                        .originProtocol("SMPP")
                        .destNetworkType("GW")
                        .destProtocol("SS7")
                        .destNetworkId(5)
                        .sourceAddrTon(1)
                        .sourceAddrNpi(4)
                        .destAddrTon(1)
                        .destAddrNpi(4)
                        .routingId(1)
                        .translationType(0)
                        .globalTitle("2222")
                        .globalTitleIndicator("GT0100")
                        .msisdn("1234")
                        .mscSsn(8)
                        .hlrSsn(6)
                        .smscSsn(8)
                        .dataCoding(8)
                        .imsi("748031234567890")
                        .networkNodeNumber("598991900032")
                        .networkNodeNumberNatureOfAddress(4)
                        .networkNodeNumberNumberingPlan(1)
                        .validityPeriod(120)
                        .mapVersion(3)
                        .networkIdToMapSri(-1)
                        .networkIdToPermanentFailure(-1)
                        .networkIdTempFailure(-1)
                        .isDlr(true)
                        .build(),
                //Message Part with reference number < 255
                MessageEvent.builder()
                        .id("1722446896082-12194920127675")
                        .messageId("1722446896081-12194920043917")
                        .systemId("smpp_sp")
                        .sourceAddr("8888")
                        .destinationAddr("1234")
                        .shortMessage("Hello!")
                        .originNetworkId(3)
                        .originNetworkType("SP")
                        .originProtocol("SMPP")
                        .destNetworkType("GW")
                        .destProtocol("SS7")
                        .destNetworkId(5)
                        .sourceAddrTon(1)
                        .sourceAddrNpi(4)
                        .destAddrTon(1)
                        .destAddrNpi(4)
                        .routingId(1)
                        .translationType(0)
                        .globalTitle("2222")
                        .globalTitleIndicator("GT0100")
                        .msisdn("1234")
                        .mscSsn(8)
                        .hlrSsn(6)
                        .smscSsn(8)
                        .dataCoding(0)
                        .imsi("748031234567890")
                        .networkNodeNumber("598991900032")
                        .networkNodeNumberNatureOfAddress(4)
                        .networkNodeNumberNumberingPlan(1)
                        .validityPeriod(120)
                        .mapVersion(3)
                        .networkIdToMapSri(-1)
                        .networkIdToPermanentFailure(-1)
                        .networkIdTempFailure(-1)
                        .msgReferenceNumber("4")
                        .totalSegment(4)
                        .segmentSequence(1)
                        .registeredDelivery(0)
                        .udhJson("{\"message\":\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Lorem ipsum dolor si\",\"0x00\":[4,4,1]}")
                        .build(),
                //Message Part with reference number > 255
                MessageEvent.builder()
                        .id("1722446896082-12194920127675")
                        .messageId("1722446896081-12194920043917")
                        .systemId("smpp_sp")
                        .sourceAddr("8888")
                        .destinationAddr("1234")
                        .shortMessage("Hello!")
                        .originNetworkId(3)
                        .originNetworkType("SP")
                        .originProtocol("SMPP")
                        .destNetworkType("GW")
                        .destProtocol("SS7")
                        .destNetworkId(5)
                        .sourceAddrTon(1)
                        .sourceAddrNpi(4)
                        .destAddrTon(1)
                        .destAddrNpi(4)
                        .routingId(1)
                        .translationType(0)
                        .globalTitle("2222")
                        .globalTitleIndicator("GT0100")
                        .msisdn("1234")
                        .mscSsn(8)
                        .hlrSsn(6)
                        .smscSsn(8)
                        .dataCoding(0)
                        .imsi("748031234567890")
                        .networkNodeNumber("598991900032")
                        .networkNodeNumberNatureOfAddress(4)
                        .networkNodeNumberNumberingPlan(1)
                        .validityPeriod(120)
                        .mapVersion(3)
                        .networkIdToMapSri(-1)
                        .networkIdToPermanentFailure(-1)
                        .networkIdTempFailure(-1)
                        .msgReferenceNumber("400")
                        .totalSegment(4)
                        .segmentSequence(1)
                        .registeredDelivery(0)
                        .udhJson("{\"message\":\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Lorem ipsum dolor si\",\"0x00\":[4,4,1]}")
                        .build()
        );
    }

    void checkCorrectMtForwardSMRequestValues(MessageEvent messageEvent, MAPDialogSms mapDialogSms) {
        assertNotNull(mapDialogSms);
        assertEquals(messageEvent.getMapVersion(), mapDialogSms.getApplicationContext().getApplicationContextVersion().getVersion());
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
    @DisplayName("Create Delivery Receipt Message when has Single MessageEvent, Error Code Mapping and Extra Information then it creates MessageEvent in Delivery Receipt Format")
    void createDeliveryReceiptMessageWhenHasMessageEventThenItCreatesMessageEventInDeliveryReceiptFormat() {
        MessageEvent currentMessageEvent = MessageEvent.builder()
                .id("1722446896082-12194920127675")
                .messageId("1722446896081-12194920043917")
                .systemId("smpp_sp")
                .sourceAddr("8888")
                .destinationAddr("1234")
                .shortMessage("Hello!")
                .originNetworkId(3)
                .originNetworkType("SP")
                .originProtocol("SMPP")
                .destNetworkType("GW")
                .destProtocol("SS7")
                .destNetworkId(5)
                .sourceAddrTon(1)
                .sourceAddrNpi(4)
                .destAddrTon(1)
                .destAddrNpi(4)
                .routingId(1)
                .translationType(0)
                .globalTitle("2222")
                .globalTitleIndicator("GT0100")
                .msisdn("1234")
                .mscSsn(8)
                .hlrSsn(6)
                .smscSsn(8)
                .dataCoding(0)
                .imsi("748031234567890")
                .networkNodeNumber("598991900032")
                .networkNodeNumberNatureOfAddress(4)
                .networkNodeNumberNumberingPlan(1)
                .validityPeriod(120)
                .mapVersion(3)
                .networkIdToMapSri(-1)
                .networkIdToPermanentFailure(-1)
                .networkIdTempFailure(-1)
                .build();
        var deliverMessageEventSingle = this.messageFactory.createDeliveryReceiptMessage(currentMessageEvent, null, null);
        this.checkCorrectDeliveryReceiptMessage(currentMessageEvent, deliverMessageEventSingle);

        ErrorCodeMapping defaultMapping = new ErrorCodeMapping();
        defaultMapping.setErrorCode(0);
        defaultMapping.setDeliveryErrorCode(0);
        defaultMapping.setDeliveryStatus("UNDELIV");
        var deliverMessageEventWithErrorCode = this.messageFactory.createDeliveryReceiptMessage(currentMessageEvent, defaultMapping, null);
        this.checkCorrectDeliveryReceiptMessage(currentMessageEvent, deliverMessageEventWithErrorCode);

        String extraString = " imsi:" +
                currentMessageEvent.getImsi() +
                " nnn_digits:" +
                currentMessageEvent.getNetworkNodeNumber() +
                " nnn_an:" +
                currentMessageEvent.getNetworkNodeNumberNatureOfAddress() +
                " nnn_np:" +
                currentMessageEvent.getNetworkNodeNumberNumberingPlan();
        var deliverMessageEventWithExtraInformation = this.messageFactory.createDeliveryReceiptMessage(currentMessageEvent, null, extraString);
        this.checkCorrectDeliveryReceiptMessage(currentMessageEvent, deliverMessageEventWithExtraInformation);
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
    @DisplayName("Create MessageEvent from MoForwardShortMessageRequest with different ValidityPeriodFormat")
    void createMessageEventFromMoForwardShortMessageRequestWithDifferentValidityPeriodFormat() throws MAPException {
        startLayers();
        List<MoForwardShortMessageRequest> moForwardShortMessageRequestImplList = List.of(
                MessageUtil.createMoMessage(ValidityPeriodFormat.fieldPresentRelativeFormat, this.mapLayer, 0),
                MessageUtil.createMoMessage(ValidityPeriodFormat.fieldPresentAbsoluteFormat, this.mapLayer, 0),
                MessageUtil.createMoMessage(ValidityPeriodFormat.fieldPresentEnhancedFormat, this.mapLayer, 0)
        );
        moForwardShortMessageRequestImplList.forEach(mo -> {
            try {
                this.checkCorrectMoMessageEvent(mo);
            } catch (MAPException e) {
                throw new RuntimeException(e);
            }
        });
        stopLayers();
    }

    void checkCorrectMoMessageEvent(MoForwardShortMessageRequest moForwardShortMessageRequestImpl) throws MAPException {
        SmsSignalInfo smsSignalInfo = moForwardShortMessageRequestImpl.getSM_RP_UI();
        SmsTpdu smsTpdu = smsSignalInfo.decodeTpdu(true);
        SmsSubmitTpdu smsSubmitTpdu = (SmsSubmitTpdu) smsTpdu;
        MessageEvent messageEventMo = this.messageFactory.createMessageEventFromMoForwardShortMessageRequest(moForwardShortMessageRequestImpl, smsSubmitTpdu);
        assertNotNull(messageEventMo);
        String messageId = smsSubmitTpdu.getMessageReference() + "";
        assertEquals(messageId, messageEventMo.getMessageId());
        assertEquals(messageId, messageEventMo.getParentId());
        assertEquals(0, messageEventMo.getEsmClass());
        assertTrue(messageEventMo.isMoMessage());

        ISDNAddressString originMsisdn = moForwardShortMessageRequestImpl.getSM_RP_OA().getMsisdn();
        assertEquals(originMsisdn.getAddress(), messageEventMo.getSourceAddr());
        assertEquals(CustomTypeOfNumber.fromPrimitive(originMsisdn.getAddressNature()).getSmscValue().value(), messageEventMo.getSourceAddrTon());
        assertEquals(CustomNumberingPlanIndicator.fromPrimitive(originMsisdn.getNumberingPlan()).getSmscValue().value(), messageEventMo.getSourceAddrNpi());
        assertEquals(smsSubmitTpdu.getDataCodingScheme().getCode(), messageEventMo.getDataCoding());
    }

    @Test
    @DisplayName("Create ReportSMDeliveryStatusRequest From Message Event Then It Creates Map Dialog Sms")
    void createReportSMDeliveryStatusRequestFromMessageEventThenItCreatesMapDialogSms() throws MAPException {
        startLayers();
        MAPDialogSms mapDialogSms;
        MessageEvent currentMessageEvent = MessageEvent.builder()
                .id("1722446896082-12194920127675")
                .messageId("1722446896081-12194920043917")
                .systemId("smpp_sp")
                .sourceAddr("8888")
                .destinationAddr("1234")
                .shortMessage("Hello!")
                .originNetworkId(3)
                .originNetworkType("SP")
                .originProtocol("SMPP")
                .destNetworkType("GW")
                .destProtocol("SS7")
                .destNetworkId(5)
                .sourceAddrTon(1)
                .sourceAddrNpi(4)
                .destAddrTon(1)
                .destAddrNpi(4)
                .routingId(1)
                .translationType(0)
                .globalTitle("2222")
                .globalTitleIndicator("GT0100")
                .msisdn("1234")
                .mscSsn(8)
                .hlrSsn(6)
                .smscSsn(8)
                .dataCoding(0)
                .imsi("748031234567890")
                .networkNodeNumber("598991900032")
                .networkNodeNumberNatureOfAddress(4)
                .networkNodeNumberNumberingPlan(1)
                .validityPeriod(120)
                .mapVersion(3)
                .networkIdToMapSri(-1)
                .networkIdToPermanentFailure(-1)
                .networkIdTempFailure(-1)
                .build();

        mapDialogSms = this.messageFactory.createReportSMDeliveryStatusRequestFromMessageEvent(
                currentMessageEvent, true);
        this.checkCorrectReportSMDeliveryStatusRequestValues(currentMessageEvent, mapDialogSms);

        mapDialogSms = this.messageFactory.createReportSMDeliveryStatusRequestFromMessageEvent(
                currentMessageEvent, false);
        this.checkCorrectReportSMDeliveryStatusRequestValues(currentMessageEvent, mapDialogSms);

        stopLayers();
    }

    void checkCorrectReportSMDeliveryStatusRequestValues(MessageEvent messageEvent, MAPDialogSms mapDialogSms) {
        assertNotNull(mapDialogSms);
        assertEquals(messageEvent.getMapVersion(), mapDialogSms.getApplicationContext().getApplicationContextVersion().getVersion());
        var remoteAddress = mapDialogSms.getRemoteAddress();
        var localAddress = mapDialogSms.getLocalAddress();
        assertNotNull(remoteAddress);
        assertEquals(messageEvent.getDestinationAddr(), remoteAddress.getGlobalTitle().getDigits());
        assertEquals(0, remoteAddress.getSignalingPointCode());
        assertEquals(messageEvent.getMscSsn(), remoteAddress.getSubsystemNumber());

        assertNotNull(localAddress);
        assertEquals(messageEvent.getGlobalTitle(), localAddress.getGlobalTitle().getDigits());
        assertEquals(0, localAddress.getSignalingPointCode());
        assertEquals(messageEvent.getSmscSsn(), localAddress.getSubsystemNumber());
    }

}