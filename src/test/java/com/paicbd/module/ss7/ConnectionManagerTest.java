package com.paicbd.module.ss7;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.impl.GatewayUtil;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.utils.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.JedisCluster;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionManagerTest {

    @Mock
    JedisCluster jedisCluster;

    @Mock
    ConcurrentMap<Integer, Gateway> gatewayConcurrentMap;

    @Mock
    AppProperties appProperties;

    @Mock
    ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap;

    @Mock
    CdrProcessor cdrProcessor;

    @InjectMocks
    ExtendedResource extendedResource;

    @InjectMocks
    ConnectionManager connectionManager;

    @Test
    void testInit() {
        when(appProperties.getConfigPath()).thenReturn("");
        Mockito.when(this.appProperties.getKeyGatewayRedis()).thenReturn("ss7_gateways");
        Map<String, String> gatewaysMap = new HashMap<>();
        var gateway = GatewayUtil.getGateway(8888,9999);
        String gatewayInRaw = Converter.valueAsString(gateway);
        gatewaysMap.put("1", gatewayInRaw);
        Mockito.when(this.jedisCluster.hgetAll(appProperties.getKeyGatewayRedis())).thenReturn(gatewaysMap);

        Mockito.when(this.appProperties.getKeyErrorCodeMapping()).thenReturn("error_code_mapping");
        Map<String, String> errorCodeMap = new HashMap<>();
        errorCodeMap.put("1", "[{\"error_code\":97,\"delivery_error_code\":5,\"delivery_status\":\"EXPIRED\"},{\"error_code\":88,\"delivery_error_code\":6,\"delivery_status\":\"EXPIRED\"}]'");
        Mockito.when(this.jedisCluster.hgetAll(appProperties.getKeyErrorCodeMapping())).thenReturn(errorCodeMap);

        this.gatewayConcurrentMap = new ConcurrentHashMap<>();
        this.gatewayConcurrentMap.put(1, gateway);

        this.connectionManager = new ConnectionManager(extendedResource, jedisCluster, gatewayConcurrentMap, appProperties,
                errorCodeMappingConcurrentHashMap, cdrProcessor);

        assertDoesNotThrow(() -> this.connectionManager.init());

        gatewaysMap.clear();
        errorCodeMap.clear();
        assertDoesNotThrow(() -> this.connectionManager.init());

        gatewaysMap.put("1", "{//]{");
        errorCodeMap.put("1", "[\"delivery_error_code\":5,\"delivery_status\":\"EXPIRED\"},{\"error_code\":88,\"delivery_error_code\":6,\"delivery_status\":\"EXPIRED\"}]'");
        assertDoesNotThrow(() -> this.connectionManager.init());

        assertDoesNotThrow(() -> this.connectionManager.destroy());
    }

    @Test
    void testUpdateErrorCodeMapping() {
        String errorCodeMapping = "[{\"error_code\":97,\"delivery_error_code\":5,\"delivery_status\":\"EXPIRED\"},{\"error_code\":88,\"delivery_error_code\":6,\"delivery_status\":\"EXPIRED\"}]";
        Mockito.when(this.jedisCluster.hget(this.appProperties.getKeyErrorCodeMapping(), "2")).thenReturn(errorCodeMapping);
        assertDoesNotThrow(() -> this.connectionManager.updateErrorCodeMapping("2"));
        Mockito.when(this.jedisCluster.hget(this.appProperties.getKeyErrorCodeMapping(), "3")).thenReturn(null);
        assertDoesNotThrow(() -> this.connectionManager.updateErrorCodeMapping("3"));
        Mockito.when(this.jedisCluster.hget(this.appProperties.getKeyErrorCodeMapping(), "4")).thenReturn("[{\"error_code\":97,\"delivery_error_code\"");
        assertDoesNotThrow(() -> this.connectionManager.updateErrorCodeMapping("4"));
    }

    void initConnectionManager() {
        when(appProperties.getConfigPath()).thenReturn("");
        Mockito.when(this.appProperties.getKeyGatewayRedis()).thenReturn("ss7_gateways");
        var gateway = GatewayUtil.getGateway(8888,9999);
        this.gatewayConcurrentMap = new ConcurrentHashMap<>();
        String gatewayInRaw = Converter.valueAsString(gateway);
        Mockito.when(this.jedisCluster.hget(appProperties.getKeyGatewayRedis(), "1")).thenReturn(gatewayInRaw);
        this.connectionManager = new ConnectionManager(this.extendedResource, this.jedisCluster, this.gatewayConcurrentMap,
                this.appProperties, this.errorCodeMappingConcurrentHashMap, this.cdrProcessor);
    }

    @Test
    void testAddGateway() {
        initConnectionManager();
        assertDoesNotThrow(() -> this.connectionManager.updateGateway("1"));
        assertDoesNotThrow(() -> this.connectionManager.deleteGateway("1"));
    }

    @Test
    void testUpdateGateway() {
        var gateway = GatewayUtil.getGateway(5555,7777);
        String gatewayInRaw = Converter.valueAsString(gateway);
        initConnectionManager();
        assertDoesNotThrow(() -> this.connectionManager.updateGateway("1"));
        Mockito.when(this.jedisCluster.hget(appProperties.getKeyGatewayRedis(), "1")).thenReturn(gatewayInRaw);
        assertDoesNotThrow(() -> this.connectionManager.updateGateway("1"));
        assertDoesNotThrow(() -> this.connectionManager.deleteGateway("1"));
    }

    @Test
    void testDeleteGatewayNotFound() {
        assertDoesNotThrow(() -> this.connectionManager.deleteGateway("5"));
    }
}