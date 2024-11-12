package com.paicbd.module.ss7;

import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.LayerManager;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.module.utils.GatewayCreator;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.exception.RTException;
import com.paicbd.smsc.utils.Converter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.JedisCluster;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionManagerTest {
    @Mock
    ExtendedResource extendedResource;

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
    ConnectionManager connectionManager;

    @Test
    @DisplayName("Init, when gateways and error code mapping from redis is empty, then load but maps size is zero")
    void initWhenGatewaysAndErrorCodeMappingFromRedisIsEmptyThenLoadButMapsSizeIsZero() throws NoSuchFieldException, IllegalAccessException {
        var spyGatewaysMap = spy(new ConcurrentHashMap<Integer, Gateway>());
        var spyErrorCodeMappingConcurrentHashMap = spy(new ConcurrentHashMap<String, List<ErrorCodeMapping>>());

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, spyGatewaysMap, appProperties,
                spyErrorCodeMappingConcurrentHashMap, cdrProcessor);

        when(appProperties.getKeyGatewayRedis()).thenReturn("ss7_gateways");
        when(jedisCluster.hgetAll("ss7_gateways")).thenReturn(Collections.emptyMap());
        when(appProperties.getKeyErrorCodeMapping()).thenReturn("error_code_mapping");
        when(jedisCluster.hgetAll("error_code_mapping")).thenReturn(Collections.emptyMap());

        var spyConnectionManager = spy(connectionManager);
        spyConnectionManager.init();
        assertEquals(0, spyGatewaysMap.size());
        assertEquals(0, spyErrorCodeMappingConcurrentHashMap.size());
        assertEquals(0, getConcurrentHashMap().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Init, when gateways and error code mapping from redis is not empty, then load maps")
    void initWhenGatewaysAndErrorCodeMappingNotEmptyThenLoadMaps() throws NoSuchFieldException, IllegalAccessException {
        var spyGatewaysMap = spy(new ConcurrentHashMap<Integer, Gateway>());
        var spyErrorCodeMappingConcurrentHashMap = spy(new ConcurrentHashMap<String, List<ErrorCodeMapping>>());

        Gateway ss7Gateway = GatewayCreator.buildSS7Gateway("ss7gw01", 1, 4);
        when(appProperties.getConfigPath()).thenReturn("");
        extendedResource = new ExtendedResource(appProperties);
        String path = extendedResource.createDirectory(ss7Gateway.getName());
        when(extendedResource.createDirectory(ss7Gateway.getName())).thenReturn(path);

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, spyGatewaysMap, appProperties,
                spyErrorCodeMappingConcurrentHashMap, cdrProcessor);

        ErrorCodeMapping errorCodeMapping = new ErrorCodeMapping(121, 88, "UNDELIV");


        when(appProperties.getKeyGatewayRedis()).thenReturn("ss7_gateways");
        when(jedisCluster.hgetAll("ss7_gateways")).thenReturn(
                Collections.singletonMap("1", ss7Gateway.toString()));
        when(appProperties.getKeyErrorCodeMapping()).thenReturn("error_code_mapping");
        when(jedisCluster.hgetAll("error_code_mapping")).thenReturn(
                Collections.singletonMap(ss7Gateway.getMnoId().toString(),
                        Converter.valueAsString(List.of(errorCodeMapping))));

        var spyConnectionManager = spy(connectionManager);
        spyConnectionManager.init();

        assertEquals(1, spyGatewaysMap.size());
        assertEquals(1, spyErrorCodeMappingConcurrentHashMap.size());

        ArgumentCaptor<Integer> networkIdCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Gateway> gatewayCaptor = ArgumentCaptor.forClass(Gateway.class);

        ArgumentCaptor<String> mnoIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<ErrorCodeMapping>> errorCodeMappingCaptor = ArgumentCaptor.forClass(List.class);

        verify(spyGatewaysMap).put(networkIdCaptor.capture(), gatewayCaptor.capture());
        verify(spyErrorCodeMappingConcurrentHashMap).put(mnoIdCaptor.capture(), errorCodeMappingCaptor.capture());

        assertEquals(ss7Gateway.getNetworkId(), networkIdCaptor.getValue());
        assertEquals(ss7Gateway.toString(), gatewayCaptor.getValue().toString());

        assertEquals("1", mnoIdCaptor.getValue());
        assertEquals(Converter.valueAsString(List.of(errorCodeMapping)),
                Converter.valueAsString(errorCodeMappingCaptor.getValue()));

        var layerManagerMap = getConcurrentHashMap();
        assertEquals(1, layerManagerMap.size());
    }

    @Test
    @DisplayName("Init, Invalid format for ErrorCodeMapping JSON, then thrown RTException")
    void initWhenErrorCodeMappingInvalidJsonThenThrownRTException() throws NoSuchFieldException, IllegalAccessException {
        var spyGatewaysMap = spy(new ConcurrentHashMap<Integer, Gateway>());
        var spyErrorCodeMappingConcurrentHashMap = spy(new ConcurrentHashMap<String, List<ErrorCodeMapping>>());

        Gateway ss7Gateway = GatewayCreator.buildSS7Gateway("ss7gw01", 1, 4);
        when(appProperties.getConfigPath()).thenReturn("");
        extendedResource = new ExtendedResource(appProperties);
        String path = extendedResource.createDirectory(ss7Gateway.getName());
        when(extendedResource.createDirectory(ss7Gateway.getName())).thenReturn(path);

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, spyGatewaysMap, appProperties,
                spyErrorCodeMappingConcurrentHashMap, cdrProcessor);

        when(appProperties.getKeyGatewayRedis()).thenReturn("ss7_gateways");
        when(jedisCluster.hgetAll("ss7_gateways")).thenReturn(
                Collections.singletonMap("1", ss7Gateway.toString()));

        when(appProperties.getKeyErrorCodeMapping()).thenReturn("error_code_mapping");
        when(jedisCluster.hgetAll("error_code_mapping")).thenReturn(
                Collections.singletonMap("invalid_json_x1", "invalid_json_x2"));

        var spyConnectionManager = spy(connectionManager);
        try {
            spyConnectionManager.init();
        } catch (RTException | NullPointerException e) {
            assertInstanceOf(RTException.class, e);
        }

        assertEquals(1, spyGatewaysMap.size());
        assertEquals(0, spyErrorCodeMappingConcurrentHashMap.size());
        assertEquals(1, getConcurrentHashMap().size());

        verifyNoInteractions(cdrProcessor);
    }

    @Test
    @DisplayName("Init, when gateway invalid format, then size of gateways map is zero")
    void initWhenGatewayInvalidFormatThenSizeOfGatewaysMapIsZero() {
        var spyGatewaysMap = spy(new ConcurrentHashMap<Integer, Gateway>());
        var spyErrorCodeMappingConcurrentHashMap = spy(new ConcurrentHashMap<String, List<ErrorCodeMapping>>());

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, spyGatewaysMap, appProperties,
                spyErrorCodeMappingConcurrentHashMap, cdrProcessor);

        when(appProperties.getKeyGatewayRedis()).thenReturn("ss7_gateways");
        when(jedisCluster.hgetAll("ss7_gateways")).thenReturn(
                Collections.singletonMap("1", "invalid_json"));
        assertThrows(NullPointerException.class, connectionManager::init);
    }

    @Test
    @DisplayName("Init, when gateway exists, then refresh layer manager")
    void updateGatewayWhenGatewayExistsThenRefreshLayerManager() {
        String networkIdToUpdate = "4";
        var spyGatewaysMap = spy(new ConcurrentHashMap<Integer, Gateway>());

        Gateway previousGateway = GatewayCreator.buildSS7Gateway("ss7gw01", 1, Integer.parseInt(networkIdToUpdate));
        spyGatewaysMap.put(previousGateway.getNetworkId(), previousGateway);

        Gateway newGateway = GatewayCreator.buildSS7Gateway("ss7gw01", 7, 4);

        when(appProperties.getConfigPath()).thenReturn("");
        extendedResource = new ExtendedResource(appProperties);

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, spyGatewaysMap, appProperties,
                errorCodeMappingConcurrentHashMap, cdrProcessor);

        String path = extendedResource.createDirectory(newGateway.getName());
        when(extendedResource.createDirectory(newGateway.getName())).thenReturn(path);

        when(appProperties.getKeyGatewayRedis()).thenReturn("ss7_gateways");
        when(jedisCluster.hget(appProperties.getKeyGatewayRedis(), networkIdToUpdate)).thenReturn(newGateway.toString());

        var spyConnectionManager = spy(connectionManager);
        spyConnectionManager.init();
        spyConnectionManager.updateGateway(networkIdToUpdate);
        verify(spyGatewaysMap).containsKey(Integer.parseInt(networkIdToUpdate));

        Gateway capturedGateway = spyGatewaysMap.get(Integer.parseInt(networkIdToUpdate));

        assertEquals(1, spyGatewaysMap.size());
        assertEquals(newGateway.toString(), capturedGateway.toString());
    }

    @Test
    @DisplayName("Update Gateway, when gateway does not exist, then add gateway")
    void updateGatewayWhenGatewayDoesNotExistThenAddGateway() {
        String networkIdToUpdate = "4";
        var spyGatewaysMap = spy(new ConcurrentHashMap<Integer, Gateway>());

        when(appProperties.getConfigPath()).thenReturn("");
        extendedResource = new ExtendedResource(appProperties);

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, spyGatewaysMap, appProperties,
                errorCodeMappingConcurrentHashMap, cdrProcessor);

        Gateway newGateway = GatewayCreator.buildSS7Gateway("gwNetId4", 7, 4);

        when(appProperties.getKeyGatewayRedis()).thenReturn("ss7_gateways");
        when(jedisCluster.hget(appProperties.getKeyGatewayRedis(), networkIdToUpdate)).thenReturn(newGateway.toString());

        var spyConnectionManager = spy(connectionManager);
        spyConnectionManager.init();
        spyConnectionManager.updateGateway(networkIdToUpdate);
        verify(spyGatewaysMap).containsKey(Integer.parseInt(networkIdToUpdate));

        Gateway capturedGateway = spyGatewaysMap.get(Integer.parseInt(networkIdToUpdate));

        assertEquals(1, spyGatewaysMap.size());
        assertEquals(newGateway.toString(), capturedGateway.toString());
    }

    @Test
    @DisplayName("Update Gateway, when the mnoId does not exist, then if the key exists in the map, then remove it")
    void updateErrorCodeMappingWhenGetNullFromRedisThenRemoveItFromMap() {
        ErrorCodeMapping currentErrorCodeMapping = new ErrorCodeMapping(121, 88, "UNDELIV");
        var spyErrorCodeMappingConcurrentHashMap = spy(new ConcurrentHashMap<String, List<ErrorCodeMapping>>());
        spyErrorCodeMappingConcurrentHashMap.put("1", List.of(currentErrorCodeMapping));

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, gatewayConcurrentMap, appProperties,
                spyErrorCodeMappingConcurrentHashMap, cdrProcessor);

        when(appProperties.getKeyErrorCodeMapping()).thenReturn("error_code_mapping");
        when(jedisCluster.hget(appProperties.getKeyErrorCodeMapping(), "1")).thenReturn(null);

        connectionManager.updateErrorCodeMapping("1");

        verify(spyErrorCodeMappingConcurrentHashMap).remove("1");
        assertEquals(0, spyErrorCodeMappingConcurrentHashMap.size());
    }

    @Test
    @DisplayName("Update ErrorCodeMapping, when get list from redis, then do it successfully")
    void updateErrorCodeMappingWhenGetListFromRedisThenDoItSuccessfully() {
        ErrorCodeMapping currentErrorCodeMapping = new ErrorCodeMapping(121, 88, "UNDELIV");
        var spyErrorCodeMappingConcurrentHashMap = spy(new ConcurrentHashMap<String, List<ErrorCodeMapping>>());
        spyErrorCodeMappingConcurrentHashMap.put("1", List.of(currentErrorCodeMapping));

        ErrorCodeMapping additionalErrorCodeMapping = new ErrorCodeMapping(122, 89, "UNDELIV");
        when(appProperties.getKeyErrorCodeMapping()).thenReturn("error_code_mapping");
        when(jedisCluster.hget(appProperties.getKeyErrorCodeMapping(), "1")).thenReturn(
                Converter.valueAsString(List.of(currentErrorCodeMapping, additionalErrorCodeMapping)));

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, gatewayConcurrentMap, appProperties,
                spyErrorCodeMappingConcurrentHashMap, cdrProcessor);

        connectionManager.updateErrorCodeMapping("1");

        assertEquals(1, spyErrorCodeMappingConcurrentHashMap.size());
        assertTrue(spyErrorCodeMappingConcurrentHashMap.containsKey("1"));
        assertEquals(2, spyErrorCodeMappingConcurrentHashMap.get("1").size());
    }

    @Test
    @DisplayName("Update ErrorCodeMapping, when get key error code mapping throws exception, then thrown RTException")
    void updateErrorCodeMappingWhenGetKeyErrorCodeMappingThrowsExceptionThenThrownRTException() {
        when(appProperties.getKeyErrorCodeMapping()).thenThrow(new RTException("Error"));
        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, gatewayConcurrentMap, appProperties,
                errorCodeMappingConcurrentHashMap, cdrProcessor);

        connectionManager.updateErrorCodeMapping("1");

        verifyNoInteractions(errorCodeMappingConcurrentHashMap);
    }

    @Test
    @DisplayName("Destroy, when app is stopped, then stop manager")
    void destroyWhenAppIsStoppedThenStopManager() {
        var spyGatewaysMap = new ConcurrentHashMap<Integer, Gateway>();

        Gateway gateway = GatewayCreator.buildSS7Gateway("ss7gw01", 1, 4);
        spyGatewaysMap.put(gateway.getNetworkId(), gateway);

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, spyGatewaysMap, appProperties,
                errorCodeMappingConcurrentHashMap, cdrProcessor);

        when(appProperties.getKeyGatewayRedis()).thenReturn("ss7_gateways");
        when(jedisCluster.hgetAll("ss7_gateways")).thenReturn(Collections.singletonMap("1", gateway.toString()));
        when(appProperties.getKeyErrorCodeMapping()).thenReturn("error_code_mapping");
        when(jedisCluster.hgetAll("error_code_mapping")).thenReturn(
                Collections.singletonMap(gateway.getMnoId().toString(),
                        Converter.valueAsString(List.of(new ErrorCodeMapping(121, 88, "UNDELIV")))));
        when(extendedResource.createDirectory(anyString())).thenReturn("/tmp/ss7GW07");

        connectionManager.init();
        connectionManager.destroy();

        verify(extendedResource).deleteDirectory(any(File.class));
    }

    @Test
    @DisplayName("Delete Gateway, when gateway exists, then remove gateway and stop layer")
    void deleteGatewayWhenGatewayExistsThenRemoveGateway() {
        var spyGatewaysMap = spy(new ConcurrentHashMap<Integer, Gateway>());

        Gateway gateway = GatewayCreator.buildSS7Gateway("ss7gw01", 1, 4);
        spyGatewaysMap.put(gateway.getNetworkId(), gateway);

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, spyGatewaysMap, appProperties,
                errorCodeMappingConcurrentHashMap, cdrProcessor);

        connectionManager = spy(connectionManager);
        doNothing().when(connectionManager).stopLayer(4);

        connectionManager.deleteGateway("4");

        verify(spyGatewaysMap).remove(4);
        verify(connectionManager).stopLayer(4);

        assertEquals(0, spyGatewaysMap.size());
        assertNull(spyGatewaysMap.get(4));
    }

    @Test
    @DisplayName("Delete Gateway, when gateway does not exist, then do nothing")
    void deleteGatewayWhenGatewayDoesNotExistThenDoNothing() {
        var spyGatewaysMap = spy(new ConcurrentHashMap<Integer, Gateway>());

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, spyGatewaysMap, appProperties,
                errorCodeMappingConcurrentHashMap, cdrProcessor);

        connectionManager.deleteGateway("4");

        verify(spyGatewaysMap).remove(4);
    }

    @Test
    @DisplayName("Manage Socket, when layer manager is null, then log warning")
    void manageSocketWhenLayerManagerIsNullThenLogWarning() {
        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, gatewayConcurrentMap, appProperties,
                errorCodeMappingConcurrentHashMap, cdrProcessor);
        connectionManager = spy(connectionManager);
        connectionManager.manageSocket("1", 1, false);
        verify(connectionManager).manageSocket("1", 1, false);
    }

    @Test
    @DisplayName("Manage Socket, when layer manager is not null, then manage socket")
    void manageSocketWhenLayerManagerIsNotNullThenManageSocket() {
        var spyGatewaysMap = new ConcurrentHashMap<Integer, Gateway>();

        Gateway gateway = GatewayCreator.buildSS7Gateway("ss7gw01", 1, 4);
        when(appProperties.getConfigPath()).thenReturn("");
        extendedResource = new ExtendedResource(appProperties);
        String path = extendedResource.createDirectory(gateway.getName());
        when(extendedResource.createDirectory(gateway.getName())).thenReturn(path);

        spyGatewaysMap.put(gateway.getNetworkId(), gateway);

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, spyGatewaysMap, appProperties,
                errorCodeMappingConcurrentHashMap, cdrProcessor);

        when(appProperties.getKeyGatewayRedis()).thenReturn("ss7_gateways");
        when(jedisCluster.hgetAll("ss7_gateways")).thenReturn(Collections.singletonMap("1", gateway.toString()));
        when(appProperties.getKeyErrorCodeMapping()).thenReturn("error_code_mapping");
        when(jedisCluster.hgetAll("error_code_mapping")).thenReturn(
                Collections.singletonMap(gateway.getMnoId().toString(),
                        Converter.valueAsString(List.of(new ErrorCodeMapping(121, 88, "UNDELIV")))));

        connectionManager.init();
        connectionManager = spy(connectionManager);
        connectionManager.manageSocket("4", 1, false);
        verify(connectionManager).manageSocket("4", 1, false);
    }

    @Test
    @DisplayName("Manage Association, when layer manager is null, then log warning")
    void manageAssociationWhenLayerManagerIsNullThenLogWarning() {
        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, gatewayConcurrentMap, appProperties,
                errorCodeMappingConcurrentHashMap, cdrProcessor);
        connectionManager = spy(connectionManager);
        connectionManager.manageAssociation("1", "assoc", false);
        verify(connectionManager).manageAssociation("1", "assoc", false);
    }

    @Test
    @DisplayName("Manage Association, when layer manager is not null, then manage association")
    void manageAssociationWhenLayerManagerIsNotNullThenManageAssociation() {
        var spyGatewaysMap = new ConcurrentHashMap<Integer, Gateway>();

        Gateway gateway = GatewayCreator.buildSS7Gateway("ss7gw01", 1, 4);
        spyGatewaysMap.put(gateway.getNetworkId(), gateway);

        connectionManager = new ConnectionManager(
                extendedResource, jedisCluster, spyGatewaysMap, appProperties,
                errorCodeMappingConcurrentHashMap, cdrProcessor);

        when(appProperties.getKeyGatewayRedis()).thenReturn("ss7_gateways");
        when(jedisCluster.hgetAll("ss7_gateways")).thenReturn(Collections.singletonMap("1", gateway.toString()));
        when(appProperties.getKeyErrorCodeMapping()).thenReturn("error_code_mapping");
        when(jedisCluster.hgetAll("error_code_mapping")).thenReturn(
                Collections.singletonMap(gateway.getMnoId().toString(),
                        Converter.valueAsString(List.of(new ErrorCodeMapping(121, 88, "UNDELIV")))));
        when(extendedResource.createDirectory(anyString())).thenReturn("/tmp/ss7GW07");
        connectionManager.init();
        connectionManager = spy(connectionManager);
        connectionManager.manageAssociation("4", "assoc", false);
        verify(connectionManager).manageAssociation("4", "assoc", false);
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<Integer, LayerManager> getConcurrentHashMap() throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = connectionManager.getClass();
        var field = clazz.getDeclaredField("layerManagerMap");
        field.setAccessible(true);
        return (ConcurrentHashMap<Integer, LayerManager>) field.get(connectionManager);
    }
}