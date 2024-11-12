package com.paicbd.module.ss7;

import com.fasterxml.jackson.core.type.TypeReference;
import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.LayerManager;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.ExtendedResource;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.exception.RTException;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionManager {
    private final ConcurrentHashMap<Integer, LayerManager> layerManagerMap = new ConcurrentHashMap<>();

    private final ExtendedResource extendedResource;
    private final JedisCluster jedisCluster;
    private final ConcurrentMap<Integer, Gateway> gatewayConcurrentMap;
    private final AppProperties appProperties;
    private final ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap;
    private final CdrProcessor cdrProcessor;

    @PostConstruct
    public void init() {
        this.loadGateways();
        this.loadErrorCodeMapping();
        this.startLayers();
    }

    private void loadGateways() {
        var gatewaysMaps = this.jedisCluster.hgetAll(this.appProperties.getKeyGatewayRedis());
        if (gatewaysMaps.isEmpty()) {
            log.warn("No gateways found for connect");
            return;
        }
        gatewaysMaps.values().forEach(gatewayInRaw -> {
            Gateway gateway = Converter.stringToObject(gatewayInRaw, Gateway.class);
            Objects.requireNonNull(gateway, "An error occurred while casting the gateway object in the loadGateways method");
            if (gateway.getEnabled() == 1) {
                this.gatewayConcurrentMap.put(gateway.getNetworkId(), gateway);
            }
        });
        log.warn("{} Gateways load successfully", this.gatewayConcurrentMap.size());
    }

    private void loadErrorCodeMapping() {
        var errorCodeMappingMap = this.jedisCluster.hgetAll(this.appProperties.getKeyErrorCodeMapping());
        if (errorCodeMappingMap.isEmpty()) {
            log.warn("No Error code mapping found");
            return;
        }
        errorCodeMappingMap.forEach((key, errorCodeMappingInRaw) -> {
            try {
                List<ErrorCodeMapping> errorCodeMappingList;
                errorCodeMappingList = Converter.stringToObject(errorCodeMappingInRaw, new TypeReference<>() {
                });
                this.errorCodeMappingConcurrentHashMap.put(key, errorCodeMappingList);
            } catch (RTException ex) {
                log.error("Error on load the error code mapping on method loadErrorCodeMapping {}", ex.getMessage(), ex);
            }
        });
        log.warn("{} Error code mapping load successfully", this.errorCodeMappingConcurrentHashMap.values().size());
    }

    private void startLayers() {
        this.gatewayConcurrentMap.values().forEach(gateway -> {
            String persistDirectoryName = gateway.getName();
            String persistDirectoryPath = this.extendedResource.createDirectory(persistDirectoryName);
            LayerManager layerManager = new LayerManager(gateway, persistDirectoryPath, this.jedisCluster, this.cdrProcessor,
                    this.appProperties, this.errorCodeMappingConcurrentHashMap);
            this.layerManagerMap.put(gateway.getNetworkId(), layerManager);
            layerManager.connect();
        });
    }

    private void stopManager() {
        layerManagerMap.values().forEach(layerManager -> {
            layerManager.stopLayerManager();
            File file = new File(layerManager.getPersistDirectory());
            extendedResource.deleteDirectory(file);
        });
    }

    @Generated
    private void waitForStart() {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            log.error("Error on sleep for congestion control");
            Thread.currentThread().interrupt();
        }
    }

    public void updateGateway(String networkIdInRaw) throws NumberFormatException {
        int networkId = Integer.parseInt(networkIdInRaw);
        String gatewayInRaw = jedisCluster.hget(appProperties.getKeyGatewayRedis(), networkIdInRaw);
        Gateway gateway = Converter.stringToObject(gatewayInRaw, Gateway.class);
        Objects.requireNonNull(gateway, "An error occurred while casting the gateway object in the updateGateway method");

        if (gatewayConcurrentMap.containsKey(gateway.getNetworkId())) {
            this.refreshLayer(gateway);
        } else {
            this.addLayer(gateway);
        }
        gatewayConcurrentMap.put(networkId, gateway);
    }

    public void deleteGateway(String networkId) {
        Gateway gateway = gatewayConcurrentMap.remove(Integer.parseInt(networkId));
        if (Objects.nonNull(gateway)) {
            stopLayer(gateway.getNetworkId());
        } else {
            log.warn("No Gateway found for networkId: {}. No action taken.", networkId);
        }
    }

    public void manageSocket(String networkId, int socketId, boolean start) {
        var layerManager = this.layerManagerMap.get(Integer.parseInt(networkId));
        if (Objects.isNull(layerManager)) {
            log.warn("No layer found for networkId: {}, to {} socket: {}", networkId, start ? "start" : "stop", socketId);
            return;
        }
        layerManager.manageSocket(socketId, start);
    }

    public void manageAssociation(String networkId, String associationName, boolean start) {
        var layerManager = this.layerManagerMap.get(Integer.parseInt(networkId));
        if (Objects.isNull(layerManager)) {
            log.warn("No layer found for networkId: {}, to {} association: {}", networkId, start ? "start" : "stop", associationName);
            return;
        }
        layerManager.manageAssociation(associationName, start);
    }

    private void refreshLayer(Gateway gateway) {
        stopLayer(gateway.getNetworkId());
        waitForStart();
        addLayer(gateway);
    }

    void stopLayer(int networkId) {
        var layer = layerManagerMap.remove(networkId);
        layer.stopLayerManager();
        File file = new File(layer.getPersistDirectory());
        extendedResource.deleteDirectory(file);
    }

    private void addLayer(Gateway gateway) {
        log.warn("Adding new layer for gateway {}", gateway.getName());
        String persistDirectoryName = gateway.getName();
        String persistDirectoryPath;
        persistDirectoryPath = extendedResource.createDirectory(persistDirectoryName);
        LayerManager layerManager = new LayerManager(gateway, persistDirectoryPath, jedisCluster, cdrProcessor,
                appProperties, errorCodeMappingConcurrentHashMap);
        layerManagerMap.put(gateway.getNetworkId(), layerManager);
        layerManager.connect();
    }

    public void updateErrorCodeMapping(String mnoId) {
        try {
            String errorCodeMappingInRaw = jedisCluster.hget(appProperties.getKeyErrorCodeMapping(), mnoId);
            if (Objects.isNull(errorCodeMappingInRaw)) {
                errorCodeMappingConcurrentHashMap.remove(mnoId); // Remove if existed, if not exist do anything
                return;
            }
            List<ErrorCodeMapping> errorCodeMappingList = Converter.stringToObject(errorCodeMappingInRaw, new TypeReference<>() {
            });
            errorCodeMappingConcurrentHashMap.put(mnoId, errorCodeMappingList); // Put do it the replacement if exist
        } catch (RTException ex) {
            log.error("Error on load the error code mapping on method updateErrorCodeMapping {}", ex.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("Destroying ConnectionManager Component");
        this.stopManager();
    }
}
