package com.paicbd.module.ss7.layer;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.module.utils.GatewayCreator;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.dto.MessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class LayerManagerTest {
    @Mock
    JedisCluster jedisCluster;

    @Mock
    CdrProcessor cdrProcessor;

    @Mock
    AppProperties appProperties;

    @Mock
    ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap;

    @Mock
    ExtendedResource extendedResource;

    LayerManager layerManager;

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Connect when data is present then process it")
    void connectWhenDataIsPresentThenProcessIt() throws InterruptedException {
        Gateway ss7Gateway = GatewayCreator.buildSS7Gateway("SS7", 1, 1);
        when(appProperties.getConfigPath()).thenReturn("");
        extendedResource = new ExtendedResource(appProperties);
        String path = extendedResource.createDirectory(ss7Gateway.getName());

        when(appProperties.getWorkersPerGateway()).thenReturn(1);

        layerManager = spy(new LayerManager(ss7Gateway, path, jedisCluster,
                cdrProcessor, appProperties, errorCodeMappingConcurrentHashMap));

        MessageEvent messageEvent = MessageEvent.builder()
                .id("1722446896082-12194920127675")
                .messageId("1722446896081-12194920043917")
                .systemId("smppsp")
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
                .sourceAddrNpi(1)
                .destAddrTon(1)
                .destAddrNpi(1)
                .routingId(1)
                .localDialogId(1234L)
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

        List<String> mockList = List.of(
                messageEvent.toString(),
                "{invalidMessage}"
        );

        when(appProperties.getTpsPerGw()).thenReturn(1);
        when(appProperties.getGatewaysWorkExecuteEvery()).thenReturn(1000);
        when(jedisCluster.llen(anyString())).thenReturn(1L);
        when(appProperties.getWorkersPerGateway()).thenReturn(1);
        when(jedisCluster.lpop(anyString(), anyInt())).thenReturn(mockList).thenReturn(null);

        layerManager.connect();
        toSleep();

        ArgumentCaptor<List<MessageEvent>> messageEventCaptor = ArgumentCaptor.forClass(List.class);
        verify(layerManager).sendMessage(messageEventCaptor.capture());
        List<MessageEvent> messageEvents = messageEventCaptor.getValue();

        assertEquals(1, messageEvents.size());
        MessageEvent firstMessage = messageEvents.getFirst();
        assertEquals("1722446896082-12194920127675", firstMessage.getId());
        assertEquals("1722446896081-12194920043917", firstMessage.getMessageId());
        assertEquals("smppsp", firstMessage.getSystemId());
        assertEquals("8888", firstMessage.getSourceAddr());
        assertEquals("1234", firstMessage.getDestinationAddr());
        assertEquals("Hello!", firstMessage.getShortMessage());
    }

    static void toSleep() throws InterruptedException {
        try (ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()) {
            CountDownLatch latch = new CountDownLatch(1);
            scheduler.schedule(latch::countDown, 2, TimeUnit.SECONDS);
            latch.await();
            scheduler.shutdown();
        }
    }
}