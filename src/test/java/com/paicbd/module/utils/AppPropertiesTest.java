package com.paicbd.module.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AppPropertiesTest {

    @InjectMocks
    private AppProperties appProperties;

    @BeforeEach
    void setUp() throws Exception {
        injectField("redisNodes", Arrays.asList("node1", "node2", "node3"));
        injectField("redisMaxTotal", 20);
        injectField("redisMaxIdle", 20);
        injectField("redisMinIdle", 1);
        injectField("redisBlockWhenExhausted", true);

        injectField("wsHost", "localhost");
        injectField("wsPort", 8080);
        injectField("wsPath", "/ws");
        injectField("wsEnabled", true);
        injectField("wsHeaderName", "header_name");
        injectField("wsHeaderValue", "header_value");
        injectField("wsRetryInterval", 10);

        injectField("keyGatewayRedis", "ss7_gateways");
        injectField("redisMessageRetryQueue", "sms_retry");
        injectField("redisMessageList", "preMessage");
        injectField("keyErrorCodeMapping", "error_code_mapping");

        injectField("configPath", "/opt/paicbd/resources/");
        injectField("workersPerGateway", 10);
        injectField("gatewaysWorkExecuteEvery", 1000);
        injectField("tpsPerGw", 1000);
    }

    @Test
    void testPropertiesRedis() {
        List<String> expectedRedisNodes = Arrays.asList("node1", "node2", "node3");
        assertEquals(expectedRedisNodes, appProperties.getRedisNodes());
        assertEquals(20, appProperties.getRedisMaxTotal());
        assertEquals(20, appProperties.getRedisMaxIdle());
        assertEquals(1, appProperties.getRedisMinIdle());
        assertTrue(appProperties.isRedisBlockWhenExhausted());
    }

    @Test
    void testPropertiesSocket() {
        assertEquals("localhost", appProperties.getWsHost());
        assertEquals(8080, appProperties.getWsPort());
        assertEquals("/ws", appProperties.getWsPath());
        assertTrue(appProperties.isWsEnabled());
        assertEquals("header_name", appProperties.getWsHeaderName());
        assertEquals("header_value", appProperties.getWsHeaderValue());
        assertEquals(10, appProperties.getWsRetryInterval());
    }

    @Test
    void testPropertiesRedisKey() {
        assertEquals("ss7_gateways", appProperties.getKeyGatewayRedis());
        assertEquals("sms_retry", appProperties.getRedisMessageRetryQueue());
        assertEquals("preMessage", appProperties.getRedisMessageList());
        assertEquals("error_code_mapping", appProperties.getKeyErrorCodeMapping());
    }

    @Test
    void testPropertiesModuleConfig() {
        assertEquals("/opt/paicbd/resources/", appProperties.getConfigPath());
        assertEquals(10, appProperties.getWorkersPerGateway());
        assertEquals(1000, appProperties.getGatewaysWorkExecuteEvery());
        assertEquals(1000, appProperties.getTpsPerGw());
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = AppProperties.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(appProperties, value);
    }

}