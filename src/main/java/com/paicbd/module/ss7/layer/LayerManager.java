package com.paicbd.module.ss7.layer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.paicbd.module.dto.Gateway;
import com.paicbd.module.ss7.layer.impl.channel.MapChannel;
import com.paicbd.module.ss7.layer.api.network.ILayer;
import com.paicbd.module.ss7.layer.impl.network.LayerFactory;
import com.paicbd.module.ss7.layer.impl.network.layers.MapLayer;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.module.utils.Ss7Utils;
import com.paicbd.smsc.cdr.CdrProcessor;
import com.paicbd.smsc.dto.ErrorCodeMapping;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.exception.RTException;
import com.paicbd.smsc.utils.Converter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import redis.clients.jedis.JedisCluster;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class LayerManager {
    private final LinkedHashMap<String, ILayer> layers = new LinkedHashMap<>();
    private final Gateway currentGateway;
    @Getter
    private final String persistDirectory;
    private final JedisCluster jedisCluster;
    private final CdrProcessor cdrProcessor;
    private MapChannel mapChannel;

    private final ExecutorService mainExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final AppProperties appProperties;
    private final String redisListName;
    private final AtomicBoolean isUp = new AtomicBoolean(false);
    private final ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap;

    public LayerManager(Gateway currentGateway, String persistDirectory, JedisCluster jedisCluster,
                        CdrProcessor cdrProcessor, AppProperties appProperties, ConcurrentMap<String, List<ErrorCodeMapping>> errorCodeMappingConcurrentHashMap) {
        this.currentGateway = currentGateway;
        this.persistDirectory = persistDirectory;
        this.jedisCluster = jedisCluster;
        this.cdrProcessor = cdrProcessor;
        this.appProperties = appProperties;
        this.redisListName = currentGateway.getNetworkId() + "_ss7_message";
        this.errorCodeMappingConcurrentHashMap = errorCodeMappingConcurrentHashMap;
    }

    public void connect() throws RTException {
        setUpLayers();
        setUpChannels();
        this.isUp.set(true);
        this.processor();
    }

    public void stopLayerManager() {
        log.info("Instance {} is stopping", currentGateway.getName());
        this.shutDownAllLayers();
        this.isUp.set(false);
    }

    private void setUpLayers() throws RTException {
        String previousLayerName = "";
        for (Ss7Utils.LayerType layerType : Ss7Utils.LayerType.values()) {
            String layerName = this.currentGateway.getName() + "-" + layerType.name();
            ILayer layerInterface = null;
            if (Ss7Utils.LayerType.SCTP.equals(layerType)) {
                layerInterface = LayerFactory.createLayerInstance(layerName, layerType, currentGateway, persistDirectory);
                layerInterface.start();
            } else {
                ILayer layerTransportInterface = layers.get(previousLayerName);
                if (Objects.nonNull(layerTransportInterface)) {
                    log.info("Layer '{}', type '{}', transport '{}' is initializing...", layerName, layerType, layerTransportInterface.getName());
                    layerInterface =
                            LayerFactory.createLayerInstance(layerName, layerType, currentGateway, persistDirectory, layerTransportInterface);
                    layerInterface.start();
                } else {
                    log.error("Layer '{}', initialization failure! Underlying transport '{}' not initialized.", layerName, previousLayerName);
                }
            }
            if (layerInterface != null) {
                layers.put(layerName, layerInterface);
                previousLayerName = layerName;
            }
        }
    }

    private void setUpChannels() {
        try {
            this.mapChannel = new MapChannel(this.jedisCluster, this.currentGateway, this.cdrProcessor,
                    this.appProperties.getRedisMessageRetryQueue(), this.appProperties.getRedisMessageList(), errorCodeMappingConcurrentHashMap);

            var mapLayer = (MapLayer) this.layers.get(this.currentGateway.getName() + "-MAP");
            mapLayer.setChannelHandler(mapChannel);
            this.mapChannel.channelInitialize(mapLayer);

        } catch (RTException e) {
            log.error("Channel of gateway {} not started", this.currentGateway.getName(), e);
        }
    }

    private void processor() {
        CompletableFuture.runAsync(() -> Flux.interval(Duration.ofMillis(this.appProperties.getGatewaysWorkExecuteEvery()))
                .flatMap(f -> {
                    if (!this.isUp.get()) {
                        return Flux.empty();
                    }
                    return fetchAllItems()
                            .doOnNext(this::sendMessage);
                })
                .subscribe(), mainExecutor);
    }

    private Flux<List<MessageEvent>> fetchAllItems() {
        int batchSize = batchPerWorker();
        if (batchSize <= 0) {
            return Flux.empty();
        }
        return Flux.range(0, appProperties.getWorkersPerGateway())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(worker -> {
                    List<String> batch = jedisCluster.lpop(redisListName, batchSize);
                    if (Objects.isNull(batch) || batch.isEmpty()) {
                        return Flux.empty();
                    }
                    List<MessageEvent> messageList = batch
                            .stream().parallel()
                            .map(message -> Converter.stringToObject(message, new TypeReference<MessageEvent>() {
                            }))
                            .toList();
                    return Flux.just(messageList);
                }).subscribeOn(Schedulers.boundedElastic());
    }

    private int batchPerWorker() {
        int recordsToTake = appProperties.getTpsPerGw() * (appProperties.getGatewaysWorkExecuteEvery() / 1000);
        int listSize = (int) jedisCluster.llen(redisListName);
        int min = Math.min(recordsToTake, listSize);
        var bpw = min / appProperties.getWorkersPerGateway();
        return bpw > 0 ? bpw : 1;
    }


    private void sendMessage(List<MessageEvent> events) {
        Flux.fromIterable(events)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(message -> {
                    try {
                        this.mapChannel.sendMessage(message);
                    } catch (Exception exception) {
                        log.error("Error on send Message on method sendMessage");
                    }
                }).subscribe();
    }

    private void shutDownAllLayers() {
        List<String> keys = new ArrayList<>(layers.keySet());
        keys = keys.reversed();
        ILayer layerToStop = null;
        try {
            for (String key : keys) {
                layerToStop = layers.get(key);
                log.warn("Stopping Layer {}", key);
                layerToStop.stop();
            }
        } catch (Exception e) {
            if (layerToStop != null) {
                log.error("Failed to stop '{}' Layer {}", layerToStop.getName(), e.getMessage());
            } else {
                log.error(e.getMessage());
            }
        }
    }

}
