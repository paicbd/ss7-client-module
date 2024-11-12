package com.paicbd.module;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.e2e.MapServer;
import com.paicbd.module.e2e.MtFSMReaction;
import com.paicbd.module.e2e.SRIReaction;
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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.paicbd.module.utils.Constants.ABSENT_SUBSCRIBER_HASH_NAME;
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
    @ParameterizedTest
    @MethodSource("messageEventProvider")
    @DisplayName("Start associations and send message (SRI and MT) with success response")
    void startAssociationsAndSendMessageWithSuccessResult(MessageEvent messageEvent) throws Exception {
        when(appProperties.getConfigPath()).thenReturn("");
        extendedResource = new ExtendedResource(appProperties);
        String path = extendedResource.createDirectory("ServerTestForSriAndMt");
        MapServer mapServer = new MapServer(path, 8988, 8989);
        mapServer.initializeStack(IpChannelType.SCTP);
        sleepFlow(2000);
        Gateway gw = GatewayCreator.buildSS7Gateway("ss7Test", 1, 4, 8989, 8988);
        String listName = gw.getNetworkId() + "_ss7_message";
        when(appProperties.getGatewaysWorkExecuteEvery()).thenReturn(1000);
        LayerManager layerManager = new LayerManager(gw, path, this.jedisCluster,
                this.cdrProcessor, this.appProperties, this.errorCodeMappingConcurrentHashMap);
        when(appProperties.getWorkersPerGateway()).thenReturn(1);
        when(appProperties.getTpsPerGw()).thenReturn(1);
        when(jedisCluster.llen(listName)).thenReturn(1L).thenAnswer(invocation -> 0L);
        when(jedisCluster.lpop(listName, 1)).thenReturn(List.of(messageEvent.toString()));
        layerManager.connect();
        sleepFlow(2000);
        log.info("Gateway {} is up", gw.getName());

        //Verifying the layers
        LinkedHashMap<String, ILayer> layersMap = (LinkedHashMap<String, ILayer>) getClassInstances(layerManager, "layers");
        assertNotNull(layersMap);
        assertFalse(layersMap.isEmpty());
        assertTrue(Stream.of("ss7Test-SCTP", "ss7Test-M3UA", "ss7Test-SCCP", "ss7Test-TCAP", "ss7Test-MAP")
                .allMatch(layersMap::containsKey));

        ArgumentCaptor<UtilsRecords.CdrDetail> cdrDetailArgumentCaptor = ArgumentCaptor.forClass(UtilsRecords.CdrDetail.class);
        verify(cdrProcessor, atLeastOnce()).putCdrDetailOnRedis(cdrDetailArgumentCaptor.capture());
        var cdrDetails = cdrDetailArgumentCaptor.getAllValues();
        //Getting the position 1 because it contains the message sent
        this.executeCdrDetailsVerificationsForMessage(cdrDetails.get(1), messageEvent);
        layerManager.stopLayerManager();
        mapServer.stopStack();
        extendedResource.deleteDirectory(new File(path));
    }

    @ParameterizedTest
    @MethodSource("sriReactionAndMtFSMReaction")
    @DisplayName("Start associations and send message with absent subscriber response Then send message with success response")
    void startAssociationsAndSendMessageWithAbsentSubscriberResponseThenSendMessageWithSuccessResult(SRIReaction sriReaction, MtFSMReaction mtFSMReaction) throws Exception {
        when(appProperties.getConfigPath()).thenReturn("");
        extendedResource = new ExtendedResource(appProperties);
        String path = extendedResource.createDirectory("ServerTestForAbsentSubscriberInSri");

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
                .registeredDelivery(RequestDelivery.NON_REQUEST_DLR.getValue())
                .validityPeriod(120)
                .mapVersion(3)
                .networkIdToMapSri(-1)
                .networkIdToPermanentFailure(-1)
                .networkIdTempFailure(-1)
                .build();

        MapServer mapServer = new MapServer(path, 7988, 7989);
        mapServer.setSriReaction(sriReaction);
        mapServer.setMtFSMReaction(mtFSMReaction);
        mapServer.initializeStack(IpChannelType.SCTP);
        Gateway gw = GatewayCreator.buildSS7Gateway("ss7Test", 1, 4, 7989, 7988);
        String listName = gw.getNetworkId() + "_ss7_message";
        when(appProperties.getGatewaysWorkExecuteEvery()).thenReturn(1000);
        LayerManager layerManager = new LayerManager(gw, path, this.jedisCluster,
                this.cdrProcessor, this.appProperties, this.errorCodeMappingConcurrentHashMap);
        when(appProperties.getWorkersPerGateway()).thenReturn(1);
        when(appProperties.getTpsPerGw()).thenReturn(1);
        when(jedisCluster.llen(listName)).thenReturn(1L).thenAnswer(invocation -> 0L);
        when(jedisCluster.lpop(listName, 1)).thenReturn(List.of(messageEvent.toString()));
        Map<String, String> responseOfMessageInAbsentSubscriber = new HashMap<>();
        responseOfMessageInAbsentSubscriber.put(messageEvent.getMessageId(), messageEvent.toString());
        String key = messageEvent.getMsisdn().concat(ABSENT_SUBSCRIBER_HASH_NAME);
        when(jedisCluster.hgetAll(key)).thenReturn(responseOfMessageInAbsentSubscriber);
        layerManager.connect();
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

        sleepFlow(2000);
        mapServer.setSriReaction(SRIReaction.RETURN_SUCCESS);
        mapServer.setMtFSMReaction(MtFSMReaction.RETURN_SUCCESS);
        mapServer.sendAlertServiceCentre(messageEvent.getMsisdn());

        ArgumentCaptor<UtilsRecords.CdrDetail> cdrDetailArgumentCaptor = ArgumentCaptor.forClass(UtilsRecords.CdrDetail.class);
        verify(cdrProcessor, atLeastOnce()).putCdrDetailOnRedis(cdrDetailArgumentCaptor.capture());
        var cdrDetails = cdrDetailArgumentCaptor.getAllValues();
        assertEquals("SMPP", cdrDetails.getFirst().originProtocol());
        assertFalse(cdrDetails.getFirst().originProtocol().isEmpty());
        assertTrue(cdrDetails.getFirst().destProtocol().equalsIgnoreCase("SS7"));

        sleepFlow(4000);
        layerManager.stopLayerManager();

        mapServer.stopStack();
        extendedResource.deleteDirectory(new File(path));
    }

    @Test
    @DisplayName("Start associations and simulate MO scenarios, 3 invalid types and 1 valid type")
    void startAssociationsAndSendMoMessageDemo() throws Exception {
        // Connection Initialization
        when(appProperties.getConfigPath()).thenReturn("");
        extendedResource = new ExtendedResource(appProperties);
        String path = extendedResource.createDirectory("ServerTestForMo");

        MapServer mapServer = new MapServer(path, 5988, 5989);
        mapServer.initializeStack(IpChannelType.SCTP);
        Gateway gw = GatewayCreator.buildSS7Gateway("ss7Test", 1, 4, 5989, 5988);
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
        mapServer.sendMoForwardShortMessage(SmsTpduType.SMS_SUBMIT, new DataCodingSchemeImpl(0));
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
        assertNotNull(cdrDetail.messageId());
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

                default -> null;
            };
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            log.error("Error while getting values from getClassInstances()");
            return null;
        }
    }

    static Stream<Arguments> sriReactionAndMtFSMReaction() {
        return Stream.of(
                Arguments.of(SRIReaction.ERROR_ABSENT_SUBSCRIBER, MtFSMReaction.RETURN_SUCCESS),
                Arguments.of(SRIReaction.RETURN_SUCCESS, MtFSMReaction.ERROR_ABSENT_SUBSCRIBER)
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
                        .build(),

                //Multipart message
                MessageEvent.builder()
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
                        .registeredDelivery(RequestDelivery.NON_REQUEST_DLR.getValue())
                        .validityPeriod(120)
                        .mapVersion(3)
                        .networkIdToMapSri(-1)
                        .networkIdToPermanentFailure(-1)
                        .networkIdTempFailure(-1)
                        .messageParts(List.of(
                                MessagePart.builder()
                                        .messageId("1722446896081-12194920043918")
                                        .shortMessage("Hello World I'm the fist part of a multipart message")
                                        .msgReferenceNumber("2")
                                        .totalSegment(2)
                                        .segmentSequence(1)
                                        .udhJson("{\"message\":\"Hello World I'm the fist part of a multipart message\",\"0x00\":[2,2,1]}")
                                        .build(),
                                MessagePart.builder()
                                        .messageId("1722446896081-12194920043918")
                                        .shortMessage("Hello World I'm the second part of a multipart message")
                                        .msgReferenceNumber("2")
                                        .totalSegment(2)
                                        .segmentSequence(2)
                                        .udhJson("{\"message\":\"Hello World I'm the second part of a multipart message\",\"0x00\":[2,2,2]}")
                                        .build()
                        ))
                        .build()
        );
    }
}
