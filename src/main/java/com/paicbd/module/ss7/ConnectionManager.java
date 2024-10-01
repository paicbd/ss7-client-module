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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionManager {
    private final ExtendedResource extendedResource;
    private final JedisCluster jedisCluster;
    private final ConcurrentMap<Integer, Gateway> gatewayConcurrentMap;
    private final AppProperties appProperties;
    private final ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap;
    private final CdrProcessor cdrProcessor;
    private final ConcurrentHashMap<Integer, LayerManager> layerManagerMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        this.loadGateways();
        this.loadErrorCodeMapping();
        this.startLayers();
    }

    private void loadGateways() {
        try {
            var gatewaysMaps = this.jedisCluster.hgetAll(this.appProperties.getKeyGatewayRedis());
            if (gatewaysMaps.isEmpty()) {
                log.warn("No gateways found for connect");
                return;
            }
            gatewaysMaps.values().forEach(gatewayInRaw -> {
                Gateway gateway = Converter.stringToObject(gatewayInRaw, new TypeReference<>() {
                });
                this.gatewayConcurrentMap.put(gateway.getNetworkId(), gateway);
            });
            log.warn("{} Gateways load successfully", this.gatewayConcurrentMap.size());

        } catch (Exception e) {
            log.error("Error on loading the gateways {}", e.getMessage(), e);
        }
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
            try {
                String persistDirectoryPath = this.extendedResource.createDirectory(persistDirectoryName);
                LayerManager layerManager = new LayerManager(gateway, persistDirectoryPath, this.jedisCluster, this.cdrProcessor,
                        this.appProperties, this.errorCodeMappingConcurrentHashMap);
                this.layerManagerMap.put(gateway.getNetworkId(), layerManager);
                layerManager.connect();
            } catch (Exception e) {
                throw new RTException("Error on start Layers for gateway " + gateway.getName(), e);
            }
        });
    }

    private void stopManager() {
        layerManagerMap.values().forEach(layerManager -> {
            File file = new File(layerManager.getPersistDirectory());
            try {
                extendedResource.deleteDirectory(file);
            } catch (IOException e) {
                log.error("Error deleting persist directory", e);
            }
        });
    }

    private void waitForStart() {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            log.error("Error on sleep for congestion control");
            Thread.currentThread().interrupt();
        }
    }

    public void updateGateway(String networkId) {
        String gatewayInRaw = jedisCluster.hget(appProperties.getKeyGatewayRedis(), networkId);
        Gateway gateway = Converter.stringToObject(gatewayInRaw, new TypeReference<>() {
        });
        if (gatewayConcurrentMap.containsKey(gateway.getNetworkId())) {
            this.refreshLayer(gateway);
        } else {
            this.addLayer(gateway);
        }
        gatewayConcurrentMap.put(Integer.parseInt(networkId), gateway);
    }

    public void deleteGateway(String networkId) {
        int key = Integer.parseInt(networkId);
        if (gatewayConcurrentMap.containsKey(key)) {
            gatewayConcurrentMap.remove(key);
            stopLayer(key);
        } else {
            log.warn("No Gateway found for networkId: {}. No action taken.", networkId);
        }
    }

    private void refreshLayer(Gateway gateway) {
        stopLayer(gateway.getNetworkId());
        waitForStart();
        addLayer(gateway);
    }

    private void stopLayer(int networkId) {
        var layer = layerManagerMap.remove(networkId);
        layer.stopLayerManager();
        File file = new File(layer.getPersistDirectory());
        try {
            extendedResource.deleteDirectory(file);
        } catch (IOException e) {
            log.error("Error deleting persist directory for networkId {}", networkId, e);
        }
    }

    private void addLayer(Gateway gateway) {
        log.warn("Adding new layer for gateway {}", gateway.getName());
        String persistDirectoryName = gateway.getName();
        String persistDirectoryPath;
        try {
            persistDirectoryPath = extendedResource.createDirectory(persistDirectoryName);
            LayerManager layerManager = new LayerManager(gateway, persistDirectoryPath, jedisCluster, cdrProcessor,
                    appProperties, errorCodeMappingConcurrentHashMap);
            layerManagerMap.put(gateway.getNetworkId(), layerManager);
            layerManager.connect();
        } catch (IOException e) {
            log.error("Error adding new layer for gateway {}", gateway.getName(), e);
        }
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
