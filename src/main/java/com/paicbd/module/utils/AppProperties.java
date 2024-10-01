package com.paicbd.module.utils;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
public class AppProperties {
    @Value("${ss7.workersPerGateway}")
    private int workersPerGateway;

    @Value("${ss7.gatewaysWork.executeEvery}")
    private int gatewaysWorkExecuteEvery;

    @Value("${ss7.tps.perGw}")
    private int tpsPerGw;

    // Redis
    @Value("#{'${redis.cluster.nodes}'.split(',')}")
    private List<String> redisNodes;

    @Value("${redis.threadPool.maxTotal}")
    private int redisMaxTotal = 20;

    @Value("${redis.threadPool.maxIdle}")
    private int redisMaxIdle = 20;

    @Value("${redis.threadPool.minIdle}")
    private int redisMinIdle = 1;

    @Value("${redis.threadPool.blockWhenExhausted}")
    private boolean redisBlockWhenExhausted = true;

    @Value("${websocket.server.host}")
    private String wsHost;

    @Value("${websocket.server.port}")
    private int wsPort;

    @Value("${websocket.server.path}")
    private String wsPath;

    @Value("${websocket.server.enabled}")
    private boolean wsEnabled;

    @Value("${websocket.header.name}")
    private String wsHeaderName;

    @Value("${websocket.header.value}")
    private String wsHeaderValue;

    @Value("${websocket.retry.intervalSeconds}")
    private int wsRetryInterval;

    @Value("${ss7.key.gateways}")
    private String keyGatewayRedis;

    @Value("${redis.message.retry.queue}")
    private String redisMessageRetryQueue;

    @Value("${redis.message.list}")
    private String redisMessageList;

    @Value("${ss7.key.errorCodeMapping}")
    private String keyErrorCodeMapping;

    @Value("${ss7.config.directory}")
    private String configPath;
}
