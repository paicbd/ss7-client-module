package com.paicbd.module;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.e2e.InformServiceCentreReaction;
import com.paicbd.module.e2e.MapServer;
import com.paicbd.module.e2e.MtForwardSmReaction;
import com.paicbd.module.e2e.SendRoutingInfoForSmReaction;
import com.paicbd.module.ss7.layer.LayerManager;
import com.paicbd.module.ss7.layer.api.network.ILayer;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.module.utils.GatewayCreator;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.MessagePart;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.RequestDelivery;
import com.paicbd.smsc.utils.UtilsEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mobicents.protocols.api.IpChannelType;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.map.api.MAPException;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsTpduType;
import org.restcomm.protocols.ss7.map.smstpdu.DataCodingSchemeImpl;
import redis.clients.jedis.JedisCluster;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.paicbd.module.utils.Constants.ABSENT_SUBSCRIBER_HASH_NAME;
import static com.paicbd.module.utils.GatewayCreator.getRandomLocalPort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class Ss7ModuleApplicationTest {

    @Mock
    CdrProcessor cdrProcessor;

    @Mock
    ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap;

    @Mock
    JedisCluster jedisCluster;

    @Mock
    AppProperties appProperties;

    ExtendedResource extendedResource;

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Start associations and send message (SRI and MT) with success response")
    void startAssociationsAndSendMessageWithSuccessResult() throws Exception {
        when(appProperties.getConfigPath()).thenReturn("");
        extendedResource = new ExtendedResource(appProperties);
        String path = extendedResource.createDirectory("ServerTestForSriAndMt");
        int host = getRandomLocalPort();
        int peer = getRandomLocalPort();
        MapServer mapServer = new MapServer(path, host, peer);
        mapServer.initializeStack(IpChannelType.SCTP);
        Gateway gw = GatewayCreator.buildSS7Gateway("ss7Test", 1, 4, peer, host);
        String listName = gw.getNetworkId() + "_ss7_message";
        when(appProperties.getGatewaysWorkExecuteEvery()).thenReturn(1000);
        LayerManager layerManager = new LayerManager(gw, path, this.jedisCluster,
                this.cdrProcessor, this.appProperties, this.errorCodeMappingConcurrentHashMap);
        when(appProperties.getWorkersPerGateway()).thenReturn(1);
        when(appProperties.getTpsPerGw()).thenReturn(1);
        layerManager.connect();
        AtomicBoolean isUp = (AtomicBoolean) getClassInstances(layerManager, "isUp");
        assertNotNull(isUp);
        LinkedHashMap<String, ILayer> layersMap = (LinkedHashMap<String, ILayer>) getClassInstances(layerManager, "layers");
        assertNotNull(layersMap);
        assertFalse(layersMap.isEmpty());
        assertTrue(Stream.of("ss7Test-SCTP", "ss7Test-M3UA", "ss7Test-SCCP", "ss7Test-TCAP", "ss7Test-MAP")
                .allMatch(layersMap::containsKey));
        isUp.set(false);
        messageEventProvider().toList().forEach(messageEvent -> {
            when(jedisCluster.llen(listName)).thenReturn(1L).thenAnswer(invocation -> 0L);
            when(jedisCluster.lpop(listName, 1)).thenReturn(List.of(messageEvent.toString()));
            isUp.set(true);
            sleepFlow(2000);
            isUp.set(false);
            ArgumentCaptor<UtilsRecords.CdrDetail> cdrDetailArgumentCaptor = ArgumentCaptor.forClass(UtilsRecords.CdrDetail.class);
            verify(cdrProcessor, atLeastOnce()).putCdrDetailOnRedis(cdrDetailArgumentCaptor.capture());
            var cdrDetailsList = cdrDetailArgumentCaptor.getAllValues();
            var cdr = cdrDetailsList.stream().filter(
                    cdrDetail -> Objects.equals(cdrDetail.messageId(), messageEvent.getMessageId()) &&
                            UtilsEnum.CdrStatus.SENT.name().equals(cdrDetail.cdrStatus()) &&
                            Objects.nonNull(cdrDetail.imsi())
            ).findFirst().orElse(null);
            assertNotNull(cdr);
            this.executeCdrDetailsVerificationsForMessage(cdr, messageEvent);
        });
        layerManager.stopLayerManager();
        mapServer.stopStack();
        extendedResource.deleteDirectory(new File(path));
    }

    @Test
    @DisplayName("Start associations and send message with temporal error response Then send ReportSMDeliveryStatusRequest " +
            "and wait for AlertServiceCentreRequest to send the message again")
    void startAssociationsAndSendMessageWithTemporalErrorResponseAndSendReportSMDeliveryStatusRequest() {
        try {
            when(appProperties.getConfigPath()).thenReturn("");
            extendedResource = new ExtendedResource(appProperties);
            String path = extendedResource.createDirectory("ServerTestForAbsentSubscriber");
            MessageEvent messageEvent = MessageEvent.builder()
                    .id("1722446896082-12194920127675")
                    .messageId("1722446896081-12194920043917")
                    .systemId("smpp_sp")
                    .sourceAddr("50588888888")
                    .destinationAddr("50599999999")
                    .shortMessage("Hello!")
                    .originNetworkId(3)
                    .originNetworkType("SP")
                    .originProtocol("SMPP")
                    .destNetworkType("GW")
                    .destProtocol("SS7")
                    .destNetworkId(5)
                    .sourceAddrTon(1)
                    .sourceAddrNpi(1)
                    .destAddrTon(1)
                    .destAddrNpi(1)
                    .routingId(1)
                    .dataCoding(0)
                    .translationType(0)
                    .globalTitle("598991900535")
                    .globalTitleIndicator("GT0100")
                    .msisdn("50599999999")
                    .addressNatureMsisdn(1)
                    .numberingPlanMsisdn(1)
                    .mscSsn(8)
                    .hlrSsn(6)
                    .smscSsn(8)
                    .registeredDelivery(RequestDelivery.REQUEST_DLR.getValue())
                    .validityPeriod(120)
                    .mapVersion(3)
                    .networkIdToMapSri(-1)
                    .networkIdToPermanentFailure(-1)
                    .networkIdTempFailure(-1)
                    .build();

            int host = getRandomLocalPort();
            int peer = getRandomLocalPort();
            MapServer mapServer = new MapServer(path, host, peer);
            mapServer.initializeStack(IpChannelType.SCTP);
            Gateway gw = GatewayCreator.buildSS7Gateway("ss7Test", 1, 4, peer, host);
            String listName = gw.getNetworkId() + "_ss7_message";
            when(appProperties.getGatewaysWorkExecuteEvery()).thenReturn(1000);
            LayerManager layerManager = new LayerManager(gw, path, this.jedisCluster,
                    this.cdrProcessor, this.appProperties, this.errorCodeMappingConcurrentHashMap);
            when(appProperties.getWorkersPerGateway()).thenReturn(1);
            when(appProperties.getTpsPerGw()).thenReturn(1);
            layerManager.connect();
            AtomicBoolean isUp = (AtomicBoolean) getClassInstances(layerManager, "isUp");
            assertNotNull(isUp);
            Gateway gatewayInstance = (Gateway) getClassInstances(layerManager, "currentGateway");
            assertNotNull(gatewayInstance);
            assertNotNull(gatewayInstance.getSettingsM3UA());
            int expectedPeerPort = gatewayInstance.getSettingsM3UA().getAssociations().getAssociationList().getFirst().getPeerPort();
            String expectedName = gw.getName();
            assertEquals(gw.getSettingsM3UA().getAssociations().getAssociationList().getFirst().getPeerPort(), expectedPeerPort);
            assertFalse(gatewayInstance.getSettingsSCCP().getAddresses().isEmpty());
            assertEquals(gatewayInstance.getSettingsSCCP().getRules().size(), gw.getSettingsSCCP().getRules().size());
            assertTrue(gatewayInstance.getName().equalsIgnoreCase(expectedName));
            assertEquals(1, gatewayInstance.getMnoId());
            isUp.set(false);
            ArgumentCaptor<String> hashNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> fieldCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            signalingReactionForAlertServiceCentre().toList().forEach(arguments -> {
                var signalingArguments = arguments.get();
                SendRoutingInfoForSmReaction sendRoutingInfoForSmReaction = (SendRoutingInfoForSmReaction) signalingArguments[0];
                InformServiceCentreReaction informServiceCentreReaction = (InformServiceCentreReaction) signalingArguments[1];
                MtForwardSmReaction mtForwardSmReaction = (MtForwardSmReaction) signalingArguments[2];
                mapServer.setSendRoutingInfoForSmReaction(sendRoutingInfoForSmReaction);
                mapServer.setInformServiceCentreReaction(informServiceCentreReaction);
                mapServer.setMtForwardSMReaction(mtForwardSmReaction);
                when(jedisCluster.llen(listName)).thenReturn(1L).thenAnswer(invocation -> 0L);
                when(jedisCluster.lpop(listName, 1)).thenReturn(List.of(messageEvent.toString()));
                isUp.set(true);
                sleepFlow(2000);
                isUp.set(false);
                verify(jedisCluster, atLeastOnce()).hset(hashNameCaptor.capture(), fieldCaptor.capture(), valueCaptor.capture());
                String key = messageEvent.getMsisdn().concat(ABSENT_SUBSCRIBER_HASH_NAME);
                assertEquals(key, hashNameCaptor.getValue());
                assertEquals(messageEvent.getMessageId(), fieldCaptor.getValue());
                MessageEvent messageToRetry = Converter.stringToObject(valueCaptor.getValue(), MessageEvent.class);
                assertNotNull(messageToRetry);
                Map<String, String> responseOfMessageInAbsentSubscriber = new HashMap<>();
                responseOfMessageInAbsentSubscriber.put(messageToRetry.getMessageId(), messageToRetry.toString());
                when(jedisCluster.hgetAll(key)).thenReturn(responseOfMessageInAbsentSubscriber);
                mapServer.setSendRoutingInfoForSmReaction(SendRoutingInfoForSmReaction.RETURN_SUCCESS);
                mapServer.setInformServiceCentreReaction(InformServiceCentreReaction.MWD_NO);
                mapServer.setMtForwardSMReaction(MtForwardSmReaction.RETURN_SUCCESS);
                mapServer.sendAlertServiceCentre(messageToRetry.getMsisdn());
                sleepFlow(2000);
                ArgumentCaptor<String> keyToDeleteCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> valueDeletedCaptor = ArgumentCaptor.forClass(String.class);
                verify(jedisCluster, atLeastOnce()).hdel(keyToDeleteCaptor.capture(), valueDeletedCaptor.capture());
                assertEquals(key, keyToDeleteCaptor.getValue());
                assertEquals(messageEvent.getMessageId(), valueDeletedCaptor.getValue());
            });
            layerManager.stopLayerManager();
            mapServer.stopStack();
            extendedResource.deleteDirectory(new File(path));
        } catch (Exception e) {
            log.error("Error on run test for send message with temporal error response", e);
        }
    }

    @Test
    @DisplayName("Start associations and send message with permanent error response")
    void startAssociationsAndSendMessageWithPermanentErrorResponse() {
        try {
            when(appProperties.getConfigPath()).thenReturn("");
            extendedResource = new ExtendedResource(appProperties);
            String path = extendedResource.createDirectory("ServerTestForAbsentSubscriber");
            MessageEvent messageEvent = MessageEvent.builder()
                    .id("1722446896082-12194920127675")
                    .messageId("1722446896081-12194920043917")
                    .systemId("smpp_sp")
                    .sourceAddr("50588888888")
                    .destinationAddr("50599999999")
                    .shortMessage("Hello!")
                    .originNetworkId(3)
                    .originNetworkType("SP")
                    .originProtocol("SMPP")
                    .destNetworkType("GW")
                    .destProtocol("SS7")
                    .destNetworkId(5)
                    .sourceAddrTon(1)
                    .sourceAddrNpi(1)
                    .destAddrTon(1)
                    .destAddrNpi(1)
                    .routingId(1)
                    .dataCoding(0)
                    .translationType(0)
                    .globalTitle("598991900535")
                    .globalTitleIndicator("GT0100")
                    .msisdn("50599999999")
                    .addressNatureMsisdn(1)
                    .numberingPlanMsisdn(1)
                    .mscSsn(8)
                    .hlrSsn(6)
                    .smscSsn(8)
                    .registeredDelivery(RequestDelivery.REQUEST_DLR.getValue())
                    .validityPeriod(120)
                    .mapVersion(3)
                    .networkIdToMapSri(-1)
                    .networkIdToPermanentFailure(-1)
                    .networkIdTempFailure(-1)
                    .build();

            int host = getRandomLocalPort();
            int peer = getRandomLocalPort();
            MapServer mapServer = new MapServer(path, host, peer);
            mapServer.initializeStack(IpChannelType.SCTP);
            Gateway gw = GatewayCreator.buildSS7Gateway("ss7Test", 1, 4, peer, host);
            String listName = gw.getNetworkId() + "_ss7_message";
            when(appProperties.getGatewaysWorkExecuteEvery()).thenReturn(1000);
            this.errorCodeMappingConcurrentHashMap = new ConcurrentHashMap<>();
            String deliveryStatus = "REJECTD";
            ErrorCodeMapping errorCodeMappingForSriError = new ErrorCodeMapping();
            errorCodeMappingForSriError.setErrorCode(13);
            errorCodeMappingForSriError.setDeliveryErrorCode(13);
            errorCodeMappingForSriError.setDeliveryStatus(deliveryStatus);

            ErrorCodeMapping errorCodeMappingForMtError = new ErrorCodeMapping();
            errorCodeMappingForMtError.setErrorCode(32);
            errorCodeMappingForMtError.setDeliveryErrorCode(32);
            errorCodeMappingForMtError.setDeliveryStatus(deliveryStatus);

            this.errorCodeMappingConcurrentHashMap.put("1", List.of(errorCodeMappingForSriError, errorCodeMappingForMtError));
            LayerManager layerManager = new LayerManager(gw, path, this.jedisCluster,
                    this.cdrProcessor, this.appProperties, this.errorCodeMappingConcurrentHashMap);
            when(appProperties.getWorkersPerGateway()).thenReturn(1);
            when(appProperties.getTpsPerGw()).thenReturn(1);
            layerManager.connect();
            AtomicBoolean isUp = (AtomicBoolean) getClassInstances(layerManager, "isUp");
            assertNotNull(isUp);
            Gateway gatewayInstance = (Gateway) getClassInstances(layerManager, "currentGateway");
            assertNotNull(gatewayInstance);
            assertNotNull(gatewayInstance.getSettingsM3UA());
            int expectedPeerPort = gatewayInstance.getSettingsM3UA().getAssociations().getAssociationList().getFirst().getPeerPort();
            String expectedName = gw.getName();
            assertEquals(gw.getSettingsM3UA().getAssociations().getAssociationList().getFirst().getPeerPort(), expectedPeerPort);
            assertFalse(gatewayInstance.getSettingsSCCP().getAddresses().isEmpty());
            assertEquals(gatewayInstance.getSettingsSCCP().getRules().size(), gw.getSettingsSCCP().getRules().size());
            assertTrue(gatewayInstance.getName().equalsIgnoreCase(expectedName));
            assertEquals(1, gatewayInstance.getMnoId());
            isUp.set(false);
            ArgumentCaptor<String> listNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            for (Arguments arguments : signalingReactionForPermanentError().toList()) {
                var signalingArguments = arguments.get();
                SendRoutingInfoForSmReaction sendRoutingInfoForSmReaction = (SendRoutingInfoForSmReaction) signalingArguments[0];
                MtForwardSmReaction mtForwardSmReaction = (MtForwardSmReaction) signalingArguments[1];
                mapServer.setSendRoutingInfoForSmReaction(sendRoutingInfoForSmReaction);
                mapServer.setMtForwardSMReaction(mtForwardSmReaction);
                when(jedisCluster.llen(listName)).thenReturn(1L).thenAnswer(invocation -> 0L);
                when(jedisCluster.lpop(listName, 1)).thenReturn(List.of(messageEvent.toString()));
                isUp.set(true);
                sleepFlow(4000);
                isUp.set(false);
                verify(jedisCluster, atLeastOnce()).rpush(listNameCaptor.capture(), valueCaptor.capture());
                MessageEvent dlrMessage = Converter.stringToObject(valueCaptor.getValue(), MessageEvent.class);
                assertNotNull(dlrMessage);
                assertTrue(dlrMessage.getDelReceipt().contains(deliveryStatus));
            }
            layerManager.stopLayerManager();
            mapServer.stopStack();
            extendedResource.deleteDirectory(new File(path));
        } catch (Exception e) {
            log.error("Error on run test for send message with temporal error response", e);
        }
    }

    @Test
    @DisplayName("Start associations and simulate MO scenarios, 3 invalid types and 1 valid type")
    void startAssociationsAndSendMoMessageDemo() {
        try {
            // Connection Initialization
            when(appProperties.getConfigPath()).thenReturn("");
            extendedResource = new ExtendedResource(appProperties);
            String path = extendedResource.createDirectory("ServerTestForMo");
            int host = getRandomLocalPort();
            int peer = getRandomLocalPort();
            MapServer mapServer = new MapServer(path, host, peer);
            mapServer.initializeStack(IpChannelType.SCTP);
            Gateway gw = GatewayCreator.buildSS7Gateway("ss7Test", 1, 4, peer, host);
            when(appProperties.getGatewaysWorkExecuteEvery()).thenReturn(100000);
            when(appProperties.getRedisMessageList()).thenReturn("preMessage");
            when(appProperties.getRedisMessageRetryQueue()).thenReturn("retryQueue");
            sleepFlow(1500);

            LayerManager layerManager = new LayerManager(gw, path, this.jedisCluster, this.cdrProcessor, this.appProperties, this.errorCodeMappingConcurrentHashMap);
            sleepFlow(1500);

            layerManager.connect();
            sleepFlow(1500);


            // Tests for invalid MO scenarios
            Map<SmsTpduType, DataCodingSchemeImpl> invalidMoTestsConfiguration = Map.of(
                    SmsTpduType.SMS_COMMAND, new DataCodingSchemeImpl(0),
                    SmsTpduType.SMS_DELIVER_REPORT, new DataCodingSchemeImpl(0),
                    SmsTpduType.SMS_SUBMIT, new DataCodingSchemeImpl(21)
            );

            invalidMoTestsConfiguration.forEach((type, dataCodingScheme) -> {
                try {
                    mapServer.sendMoForwardShortMessage(type, dataCodingScheme);
                    sleepFlow(2000);
                    verifyNoInteractions(cdrProcessor, jedisCluster);
                } catch (MAPException e) {
                    log.error("Error testing invalid type {} for MO scenario", type, e);
                }
            });
            sleepFlow(2000);


            // Tests for valid MO scenario
            try {
                mapServer.sendMoForwardShortMessage(SmsTpduType.SMS_SUBMIT, new DataCodingSchemeImpl(0));
            } catch (MAPException e) {
                log.error(e.getMessage(), e);
            }
            sleepFlow(2000);

            ArgumentCaptor<UtilsRecords.CdrDetail> cdrDetailArgumentCaptor = ArgumentCaptor.forClass(UtilsRecords.CdrDetail.class);
            verify(cdrProcessor).putCdrDetailOnRedis(cdrDetailArgumentCaptor.capture());

            ArgumentCaptor<String> redisListNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> redisHashValueArgumentCaptor = ArgumentCaptor.forClass(String.class);
            verify(jedisCluster).rpush(redisListNameArgumentCaptor.capture(), redisHashValueArgumentCaptor.capture());

            UtilsRecords.CdrDetail cdrDetail = cdrDetailArgumentCaptor.getValue();
            executeCdrDetailsVerificationsForMo(cdrDetail, gw);

            String listName = redisListNameArgumentCaptor.getValue();
            String messageInRaw = redisHashValueArgumentCaptor.getValue();
            executeMoMessageVerifications(listName, messageInRaw);
            sleepFlow(1500);


            // Connection Shutdown
            layerManager.stopLayerManager();
            mapServer.stopStack();
            extendedResource.deleteDirectory(new File(path));
        } catch (Exception e) {
            log.error("Error on run test for simulate MO scenarios", e);
        }
    }


    @Test
    @DisplayName("Start associations and send multipart message (SRI and MT)")
    void startAssociationsAndSendMultipartMessage() throws Exception {
        when(appProperties.getConfigPath()).thenReturn("");
        extendedResource = new ExtendedResource(appProperties);
        String path = extendedResource.createDirectory("ServerTestForSriAndMt");
        int host = getRandomLocalPort();
        int peer = getRandomLocalPort();
        MapServer mapServer = new MapServer(path, host, peer);
        mapServer.initializeStack(IpChannelType.SCTP);
        Gateway gw = GatewayCreator.buildSS7Gateway("ss7Test", 1, 4, peer, host);
        String listName = gw.getNetworkId() + "_ss7_message";
        when(appProperties.getGatewaysWorkExecuteEvery()).thenReturn(1000);
        LayerManager layerManager = new LayerManager(gw, path, this.jedisCluster,
                this.cdrProcessor, this.appProperties, this.errorCodeMappingConcurrentHashMap);
        when(appProperties.getWorkersPerGateway()).thenReturn(1);
        when(appProperties.getTpsPerGw()).thenReturn(1);
        layerManager.connect();
        AtomicBoolean isUp = (AtomicBoolean) getClassInstances(layerManager, "isUp");
        assertNotNull(isUp);
        isUp.set(false);
        multipartMessageEventProvider().toList().forEach(arguments -> {
            MessageEvent messageEvent = (MessageEvent) arguments.get()[0];
            boolean responseMtForwardSmWithTcapContinue = (boolean) arguments.get()[1];
            MtForwardSmReaction mtForwardSmReaction = (MtForwardSmReaction) arguments.get()[2];
            UtilsEnum.CdrStatus cdrStatus = (UtilsEnum.CdrStatus) arguments.get()[3];
            Map<String, MessageEvent> messageEventMap = new HashMap<>();
            mapServer.setResponseMtForwardSmWithTcapContinue(responseMtForwardSmWithTcapContinue);
            mapServer.setMtForwardSMReaction(mtForwardSmReaction);
            when(jedisCluster.llen(listName)).thenReturn(1L).thenAnswer(invocation -> 0L);
            when(jedisCluster.lpop(listName, 1)).thenReturn(List.of(messageEvent.toString()));
            if (Objects.nonNull(messageEvent.getMessageParts())) {
                for (MessagePart msgPart : messageEvent.getMessageParts()) {
                    var messageEventPart = new MessageEvent().clone(messageEvent);
                    messageEventPart.setMessageId(msgPart.getMessageId());
                    messageEventPart.setShortMessage(msgPart.getShortMessage());
                    messageEventPart.setMsgReferenceNumber(msgPart.getMsgReferenceNumber());
                    messageEventPart.setTotalSegment(msgPart.getTotalSegment());
                    messageEventPart.setSegmentSequence(msgPart.getSegmentSequence());
                    messageEventPart.setUdhJson(msgPart.getUdhJson());
                    messageEventPart.setOptionalParameters(msgPart.getOptionalParameters());
                    messageEventPart.setNetworkNodeNumber("598991900032");
                    messageEventPart.setNetworkNodeNumberNatureOfAddress(1);
                    messageEventPart.setNetworkNodeNumberNumberingPlan(1);
                    messageEventPart.setImsi("748031234567890");
                    if (messageEventPart.getSegmentSequence() != 1) {
                        String key = messageEventPart.getParentId() +
                                "_" +
                                messageEventPart.getMsgReferenceNumber() +
                                "_" +
                                messageEventPart.getSegmentSequence() +
                                "_" +
                                messageEventPart.getTotalSegment();

                        when(jedisCluster.get(key)).thenReturn(messageEventPart.toString());
                    }
                    messageEventMap.put(messageEventPart.getMessageId(), messageEventPart);
                }
            }
            isUp.set(true);
            sleepFlow(2000);
            isUp.set(false);
            if (MtForwardSmReaction.ERROR_ABSENT_SUBSCRIBER.equals(mtForwardSmReaction)) {
                ArgumentCaptor<String> hashNameCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> fieldCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
                verify(jedisCluster, atLeastOnce()).hset(hashNameCaptor.capture(), fieldCaptor.capture(), valueCaptor.capture());
                String key = messageEvent.getMsisdn().concat(ABSENT_SUBSCRIBER_HASH_NAME);
                assertEquals(key, hashNameCaptor.getValue());
                MessageEvent messageToRetry = Converter.stringToObject(valueCaptor.getValue(), MessageEvent.class);
                assertNotNull(messageToRetry);
                Map<String, String> responseOfMessageInAbsentSubscriber = new HashMap<>();
                responseOfMessageInAbsentSubscriber.put(messageToRetry.getMessageId(), messageToRetry.toString());
                when(jedisCluster.hgetAll(key)).thenReturn(responseOfMessageInAbsentSubscriber);
                mapServer.setMtForwardSMReaction(MtForwardSmReaction.RETURN_SUCCESS);
                mapServer.sendAlertServiceCentre(messageToRetry.getMsisdn());
                sleepFlow(2000);
                ArgumentCaptor<String> keyToDeleteCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> valueDeletedCaptor = ArgumentCaptor.forClass(String.class);
                verify(jedisCluster, atLeastOnce()).hdel(keyToDeleteCaptor.capture(), valueDeletedCaptor.capture());
                assertEquals(key, keyToDeleteCaptor.getValue());
            } else {
                ArgumentCaptor<String> messageIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
                verify(cdrProcessor, atLeastOnce()).createCdr(messageIdArgumentCaptor.capture());
                ArgumentCaptor<UtilsRecords.CdrDetail> cdrDetailArgumentCaptor = ArgumentCaptor.forClass(UtilsRecords.CdrDetail.class);
                verify(cdrProcessor, atLeastOnce()).putCdrDetailOnRedis(cdrDetailArgumentCaptor.capture());
                var cdrDetailsList = cdrDetailArgumentCaptor.getAllValues();
                var cdrList = cdrDetailsList.stream().filter(
                        cdrDetail -> messageEventMap.containsKey(cdrDetail.messageId()) &&
                                cdrStatus.name().equals(cdrDetail.cdrStatus()) &&
                                Objects.nonNull(cdrDetail.imsi())
                ).toList();
                var messageIdList = messageIdArgumentCaptor.getAllValues().stream().filter(
                        messageEventMap::containsKey).toList();
                assertNotNull(messageIdList);
                assertEquals(messageEventMap.size(), messageIdList.size());
                assertNotNull(cdrList);
                messageEventMap.forEach((messageId, messageEventPart) -> {
                    var cdr = cdrDetailsList.stream().filter(
                            cdrDetail -> Objects.equals(cdrDetail.messageId(), messageEventPart.getMessageId()) &&
                                    cdrStatus.name().equals(cdrDetail.cdrStatus()) &&
                                    Objects.nonNull(cdrDetail.imsi())
                    ).findFirst().orElse(null);
                    assertNotNull(cdr);
                    assertEquals(messageEventPart.getMessageId(), cdr.messageId());
                    assertEquals("SS7", cdr.destProtocol());
                    assertEquals(cdrStatus.name(), cdr.cdrStatus());
                    assertEquals("SS7_CLIENT", cdr.module());
                });
            }


        });
        layerManager.stopLayerManager();
        mapServer.stopStack();
        extendedResource.deleteDirectory(new File(path));
    }

    private void executeMoMessageVerifications(String redisListName, String messageEventInRaw) {
        assertNotNull(redisListName);
        assertEquals("preMessage", redisListName);
        assertNotNull(messageEventInRaw);

        MessageEvent moMessage = Converter.stringToObject(messageEventInRaw, MessageEvent.class);
        assertNotNull(moMessage);

        assertTrue(moMessage.isMoMessage());

        assertNull(moMessage.getDeliverSmId());
        assertNull(moMessage.getDeliverSmServerId());
        assertNull(moMessage.getErrorCode());
        assertNull(moMessage.getDestProtocol());
        assertNull(moMessage.getDestNetworkType());
        assertNull(moMessage.getRetryDestNetworkId());
        assertNull(moMessage.getRetryNumber());
        assertNull(moMessage.getStatus());
        assertNull(moMessage.getDelReceipt());
        assertNull(moMessage.getCheckSubmitSmResponse());
        assertNull(moMessage.getRetryNumber());
        assertNull(moMessage.getRetryDestNetworkId());
        assertEquals(0, moMessage.getDestNetworkId());
        assertEquals(0, moMessage.getRoutingId());

        assertFalse(moMessage.isDlr());
        assertFalse(moMessage.isRetry());
        assertFalse(moMessage.isLastRetry());
        assertFalse(moMessage.isNetworkNotifyError());
        assertFalse(moMessage.isSriResponse());

        assertNotNull(moMessage.getMsisdn());
        assertNotNull(moMessage.getId());
    }

    private void executeCdrDetailsVerifications(UtilsRecords.CdrDetail cdrDetail) {
        assertNotNull(cdrDetail);
        long currentTime = System.currentTimeMillis();
        assertTrue(currentTime > cdrDetail.timestamp());
        assertNotNull(cdrDetail.idEvent());
        assertNotNull(cdrDetail.parentId());
        assertNotNull(cdrDetail.sourceAddr());
        assertNotNull(cdrDetail.sourceAddrTon());
        assertNotNull(cdrDetail.sourceAddrNpi());
        assertNotNull(cdrDetail.destinationAddr());
        assertNotNull(cdrDetail.destAddrTon());
        assertNotNull(cdrDetail.destAddrNpi());
        assertNotNull(cdrDetail.sccpCalledPartyAddressPointCode());
        assertNotNull(cdrDetail.sccpCallingPartyAddressPointCode());
        assertNotNull(cdrDetail.sccpCalledPartyAddressSubSystemNumber());
        assertNotNull(cdrDetail.sccpCallingPartyAddressSubSystemNumber());
        assertNotNull(cdrDetail.sccpCallingPartyAddress());
        assertNotNull(cdrDetail.message());
    }

    private void executeCdrDetailsVerificationsForMo(UtilsRecords.CdrDetail cdrDetail, Gateway gw) {
        this.executeCdrDetailsVerifications(cdrDetail);
        assertFalse(cdrDetail.isDlr());
        assertEquals(gw.getNetworkId(), cdrDetail.originNetworkId());
        assertEquals(0, cdrDetail.destNetworkId()); // Because the message is not routed and destNetworkId is primitive type
        assertEquals(0, cdrDetail.routingId());
        assertEquals("GW", cdrDetail.originNetworkType());
        assertEquals("SS7", cdrDetail.originProtocol());
        assertEquals("RECEIVED", cdrDetail.cdrStatus());
        assertEquals("SS7_CLIENT", cdrDetail.module());
        assertNull(cdrDetail.destNetworkType());
        assertNull(cdrDetail.destProtocol());
    }

    private void executeCdrDetailsVerificationsForMessage(UtilsRecords.CdrDetail cdrDetail, MessageEvent messageEvent) {
        this.executeCdrDetailsVerifications(cdrDetail);
        assertNotNull(cdrDetail.originNetworkId());
        assertEquals("GW", cdrDetail.destNetworkType());
        assertEquals(cdrDetail.destNetworkId(), messageEvent.getDestNetworkId());
        assertNotNull(cdrDetail.networkNodeNumber());
        assertNotNull(cdrDetail.imsi());
        assertEquals(messageEvent.getMessageId(), cdrDetail.messageId());
        assertEquals("SS7", cdrDetail.destProtocol());
        assertEquals("SENT", cdrDetail.cdrStatus());
        assertEquals("SS7_CLIENT", cdrDetail.module());
    }

    static void sleepFlow(long millis) {
        try (ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor()) {
            executorService.schedule(() -> {
            }, millis, TimeUnit.MILLISECONDS);
        }
    }

    private Object getClassInstances(LayerManager layerManager, String fieldName) {
        try {
            return switch (fieldName) {
                case "currentGateway" -> MethodHandles.privateLookupIn(layerManager.getClass(), MethodHandles.lookup())
                        .findVarHandle(layerManager.getClass(), fieldName, Gateway.class).get(layerManager);

                case "layers" -> MethodHandles.privateLookupIn(layerManager.getClass(), MethodHandles.lookup())
                        .findVarHandle(layerManager.getClass(), fieldName, LinkedHashMap.class).get(layerManager);

                case "isUp" -> MethodHandles.privateLookupIn(layerManager.getClass(), MethodHandles.lookup())
                        .findVarHandle(layerManager.getClass(), fieldName, AtomicBoolean.class).get(layerManager);

                default -> null;
            };
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            log.error("Error while getting values from getClassInstances()");
            return null;
        }
    }

    static Stream<Arguments> signalingReactionForAlertServiceCentre() {
        return Stream.of(
                //For SendRoutingInfoForSm Test
                Arguments.of(SendRoutingInfoForSmReaction.ERROR_ABSENT_SUBSCRIBER, InformServiceCentreReaction.MWD_NO, MtForwardSmReaction.RETURN_SUCCESS),
                Arguments.of(SendRoutingInfoForSmReaction.RETURN_SUCCESS, InformServiceCentreReaction.MWD_MNRF, MtForwardSmReaction.RETURN_SUCCESS),
                Arguments.of(SendRoutingInfoForSmReaction.RETURN_SUCCESS, InformServiceCentreReaction.MWD_MCEF, MtForwardSmReaction.RETURN_SUCCESS),

                //For MtForwardSm Test
                Arguments.of(SendRoutingInfoForSmReaction.RETURN_SUCCESS, InformServiceCentreReaction.MWD_NO, MtForwardSmReaction.ERROR_ABSENT_SUBSCRIBER),
                Arguments.of(SendRoutingInfoForSmReaction.RETURN_SUCCESS, InformServiceCentreReaction.MWD_NO, MtForwardSmReaction.ERROR_SYSTEM_FAILURE_MEMORY_CAPACITY_EXCEEDED)

        );
    }

    static Stream<Arguments> signalingReactionForPermanentError() {
        return Stream.of(
                //For SendRoutingInfoForSm Test
                Arguments.of(SendRoutingInfoForSmReaction.ERROR_CALL_BARRED, MtForwardSmReaction.RETURN_SUCCESS),
                //For MtForwardSm Test
                Arguments.of(SendRoutingInfoForSmReaction.RETURN_SUCCESS, MtForwardSmReaction.ERROR_SYSTEM_FAILURE_UNKNOWN_SERVICE_CENTRE)
        );
    }

    static Stream<MessageEvent> messageEventProvider() {
        return Stream.of(
                //Single message without request dlr
                MessageEvent.builder()
                        .id("1722446896082-12194920127675")
                        .messageId("1722446896081-12194920043917")
                        .parentId("1722446896081-12194920043917")
                        .systemId("smpp_sp")
                        .sourceAddr("50588888888")
                        .destinationAddr("50599999999")
                        .shortMessage("Hello This message does not request dlr!")
                        .originNetworkId(3)
                        .originNetworkType("SP")
                        .originProtocol("SMPP")
                        .destNetworkType("GW")
                        .destProtocol("SS7")
                        .destNetworkId(5)
                        .sourceAddrTon(1)
                        .sourceAddrNpi(1)
                        .destAddrTon(1)
                        .destAddrNpi(1)
                        .routingId(1)
                        .dataCoding(0)
                        .translationType(0)
                        .globalTitle("598991900535")
                        .globalTitleIndicator("GT0100")
                        .msisdn("50599999999")
                        .addressNatureMsisdn(1)
                        .numberingPlanMsisdn(1)
                        .mscSsn(8)
                        .hlrSsn(6)
                        .smscSsn(8)
                        .registeredDelivery(RequestDelivery.NON_REQUEST_DLR.getValue())
                        .validityPeriod(120)
                        .mapVersion(3)
                        .networkIdToMapSri(-1)
                        .networkIdToPermanentFailure(-1)
                        .networkIdTempFailure(-1)
                        .build(),

                //Single message with request dlr
                MessageEvent.builder()
                        .id("1722446896082-12194920127665")
                        .messageId("1722446896081-12194920043927")
                        .parentId("1722446896081-12194920043927")
                        .systemId("smpp_sp")
                        .sourceAddr("50588888888")
                        .destinationAddr("50599999999")
                        .shortMessage("Hello This message request dlr!")
                        .originNetworkId(3)
                        .originNetworkType("SP")
                        .originProtocol("SMPP")
                        .destNetworkType("GW")
                        .destProtocol("SS7")
                        .destNetworkId(5)
                        .sourceAddrTon(1)
                        .sourceAddrNpi(1)
                        .destAddrTon(1)
                        .destAddrNpi(1)
                        .routingId(1)
                        .dataCoding(0)
                        .translationType(0)
                        .globalTitle("598991900535")
                        .globalTitleIndicator("GT0100")
                        .msisdn("50599999999")
                        .addressNatureMsisdn(1)
                        .numberingPlanMsisdn(1)
                        .mscSsn(8)
                        .hlrSsn(6)
                        .smscSsn(8)
                        .registeredDelivery(RequestDelivery.REQUEST_DLR.getValue())
                        .validityPeriod(120)
                        .mapVersion(3)
                        .networkIdToMapSri(-1)
                        .networkIdToPermanentFailure(-1)
                        .networkIdTempFailure(-1)
                        .build()
                ,
                //DLR
                MessageEvent.builder()
                        .id("1719421854353-11028072268459")
                        .parentId("1719421854353-11028072268459")
                        .deliverSmId("10")
                        .deliverSmServerId("1719421854353-11028072268459")
                        .systemId("systemId123")
                        .sourceAddrTon(1)
                        .sourceAddrNpi(1)
                        .sourceAddr("50510201020")
                        .destAddrTon(1)
                        .destAddrNpi(1)
                        .destinationAddr("50582368999")
                        .esmClass(5)
                        .validityPeriod(60)
                        .registeredDelivery(0)
                        .dataCoding(0)
                        .smDefaultMsgId(0)
                        .shortMessage("id:1 sub:001 dlvrd:001 submit date:2101010000 done date:2101010000 stat:DELIVRD err:000 text:Test Message")
                        .originNetworkType("SP")
                        .originProtocol("SS7")
                        .status("DELIVRD")
                        .originNetworkId(2)
                        .destNetworkType("GW")
                        .destProtocol("SS7")
                        .destNetworkId(4)
                        .routingId(1)
                        .isDlr(true)
                        .dataCoding(0)
                        .translationType(0)
                        .globalTitle("598991900535")
                        .globalTitleIndicator("GT0100")
                        .msisdn("50582368999")
                        .addressNatureMsisdn(1)
                        .numberingPlanMsisdn(1)
                        .mscSsn(8)
                        .hlrSsn(6)
                        .smscSsn(8)
                        .mapVersion(3)
                        .networkIdToMapSri(-1)
                        .networkIdToPermanentFailure(-1)
                        .networkIdTempFailure(-1)
                        .delReceipt("id:1 sub:001 dlvrd:001 submit date:2101010000 done date:2101010000 stat:DELIVRD err:000 text:Test Message")
                        .build()
        );
    }

    static Stream<Arguments> multipartMessageEventProvider() {
        return Stream.of(
                //Multipart message with TCAP Continue response true
                Arguments.of(
                        getMultipartMessageEvent(),
                        true,
                        MtForwardSmReaction.RETURN_SUCCESS,
                        UtilsEnum.CdrStatus.SENT
                ),
                //Multipart message with TCAP Continue response true
                Arguments.of(
                        getMultipartMessageEvent(),
                        false,
                        MtForwardSmReaction.RETURN_SUCCESS,
                        UtilsEnum.CdrStatus.SENT
                ),
                //Multipart message with TCAP Continue response true and permanent error
                Arguments.of(
                        getMultipartMessageEvent(),
                        true,
                        MtForwardSmReaction.ERROR_SYSTEM_FAILURE_UNKNOWN_SERVICE_CENTRE,
                        UtilsEnum.CdrStatus.FAILED
                ),
                //Multipart message with TCAP Continue response true and permanent error
                Arguments.of(
                        getMultipartMessageEvent(),
                        true,
                        MtForwardSmReaction.ERROR_ABSENT_SUBSCRIBER,
                        UtilsEnum.CdrStatus.SENT
                )
        );
    }

    private static MessageEvent getMultipartMessageEvent() {
        return MessageEvent.builder()
                .id("1722446896082-12194920127674")
                .messageId("1722446896081-12194920043918")
                .parentId("1722446896081-12194920043918")
                .systemId("smpp_sp")
                .sourceAddr("50588888888")
                .destinationAddr("50599999999")
                .shortMessage("Hello World I'm the fist part of a multipart message Hello World I'm the second part of a multipart message")
                .originNetworkId(3)
                .originNetworkType("SP")
                .originProtocol("SMPP")
                .destNetworkType("GW")
                .destProtocol("SS7")
                .destNetworkId(5)
                .sourceAddrTon(1)
                .sourceAddrNpi(1)
                .destAddrTon(1)
                .destAddrNpi(1)
                .routingId(1)
                .dataCoding(0)
                .translationType(0)
                .globalTitle("598991900535")
                .globalTitleIndicator("GT0100")
                .msisdn("50599999999")
                .addressNatureMsisdn(1)
                .numberingPlanMsisdn(1)
                .mscSsn(8)
                .hlrSsn(6)
                .smscSsn(8)
                .registeredDelivery(RequestDelivery.REQUEST_DLR.getValue())
                .validityPeriod(120)
                .mapVersion(3)
                .networkIdToMapSri(-1)
                .networkIdToPermanentFailure(-1)
                .networkIdTempFailure(-1)
                .messageParts(List.of(
                        MessagePart.builder()
                                .messageId(System.currentTimeMillis() + "-" + System.nanoTime())
                                .shortMessage("Hello World I'm the fist part of a multipart message")
                                .msgReferenceNumber("2")
                                .totalSegment(2)
                                .segmentSequence(1)
                                .udhJson("{\"message\":\"Hello World I'm the fist part of a multipart message\",\"0x00\":[2,2,1]}")
                                .build(),
                        MessagePart.builder()
                                .messageId(System.currentTimeMillis() + "-" + System.nanoTime())
                                .shortMessage("Hello World I'm the second part of a multipart message")
                                .msgReferenceNumber("2")
                                .totalSegment(2)
                                .segmentSequence(2)
                                .udhJson("{\"message\":\"Hello World I'm the second part of a multipart message\",\"0x00\":[2,2,2]}")
                                .build()
                ))
                .build();
    }
}
