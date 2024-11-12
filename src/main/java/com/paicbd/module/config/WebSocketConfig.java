package com.paicbd.module.config;

import com.paicbd.module.components.CustomFrameHandler;
import com.paicbd.module.utils.AppProperties;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.utils.Generated;
import com.paicbd.smsc.ws.SocketClient;
import com.paicbd.smsc.ws.SocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static com.paicbd.module.utils.Constants.CONNECT_SS7_GATEWAY_ENDPOINT;
import static com.paicbd.module.utils.Constants.START_SS7_ASSOCIATION_ENDPOINT;
import static com.paicbd.module.utils.Constants.START_SS7_SOCKET_ENDPOINT;
import static com.paicbd.module.utils.Constants.STOP_SS7_ASSOCIATION_ENDPOINT;
import static com.paicbd.module.utils.Constants.STOP_SS7_GATEWAY_ENDPOINT;
import static com.paicbd.module.utils.Constants.STOP_SS7_SOCKET_ENDPOINT;
import static com.paicbd.module.utils.Constants.UPDATE_ERROR_CODE_MAPPING_ENDPOINT;
import static com.paicbd.module.utils.Constants.UPDATE_SS7_GATEWAY_ENDPOINT;
import static com.paicbd.module.utils.Constants.DELETE_SS7_GATEWAY_ENDPOINT;


@Slf4j
@Generated
@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {
    private final AppProperties appProperties;
    private final SocketSession socketSession;
    private final CustomFrameHandler customFrameHandler;

    @Bean
    public SocketClient socketClient() {
        List<String> topicsToSubscribe = List.of(
                UPDATE_ERROR_CODE_MAPPING_ENDPOINT,
                DELETE_SS7_GATEWAY_ENDPOINT,
                UPDATE_SS7_GATEWAY_ENDPOINT,
                START_SS7_ASSOCIATION_ENDPOINT,
                STOP_SS7_ASSOCIATION_ENDPOINT,
                START_SS7_SOCKET_ENDPOINT,
                STOP_SS7_SOCKET_ENDPOINT,
                CONNECT_SS7_GATEWAY_ENDPOINT,
                STOP_SS7_GATEWAY_ENDPOINT
        );

        UtilsRecords.WebSocketConnectionParams wsp = new UtilsRecords.WebSocketConnectionParams(
                appProperties.isWsEnabled(),
                appProperties.getWsHost(),
                appProperties.getWsPort(),
                appProperties.getWsPath(),
                topicsToSubscribe,
                appProperties.getWsHeaderName(),
                appProperties.getWsHeaderValue(),
                appProperties.getWsRetryInterval(),
                "SS7-MODULE"
        );
        return new SocketClient(customFrameHandler, wsp, socketSession);
    }
}
