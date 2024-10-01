package com.paicbd.module.config;

import com.paicbd.module.ss7.ConnectionManager;
import com.paicbd.smsc.ws.FrameHandler;
import com.paicbd.smsc.ws.SocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.paicbd.module.utils.Constants.RESPONSE_SS7_CLIENT_ENDPOINT;
import static com.paicbd.module.utils.Constants.DELETE_SS7_GATEWAY_ENDPOINT;
import static com.paicbd.module.utils.Constants.UPDATE_ERROR_CODE_MAPPING_ENDPOINT;
import static com.paicbd.module.utils.Constants.UPDATE_SS7_GATEWAY_ENDPOINT;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomFrameHandler implements FrameHandler {

    private final SocketSession socketSession;
    private final ConnectionManager connectionManager;

    @Override
    public void handleFrameLogic(StompHeaders headers, Object payload) {
        String payloadId = payload.toString();
        String destination = headers.getDestination();
        Objects.requireNonNull(payloadId, "PayloadId cannot be null");
        Objects.requireNonNull(destination, "Destination cannot be null");

        switch (destination) {
            case UPDATE_SS7_GATEWAY_ENDPOINT -> handleUpdateGateway(payloadId);
            case DELETE_SS7_GATEWAY_ENDPOINT -> handleDeleteGateway(payloadId);
            // Payload must be mno_id
            case UPDATE_ERROR_CODE_MAPPING_ENDPOINT -> handleUpdateErrorCodeMapping(payloadId);
            default -> log.warn("Received notification for unknown destination {}", destination);
        }
    }

    private void handleUpdateGateway(String networkId) {
        log.info("Updating gateway {}", networkId);
        this.connectionManager.updateGateway(networkId);
        this.socketSession.getStompSession().send(RESPONSE_SS7_CLIENT_ENDPOINT, "The gateway has been updated");
    }

    private void handleDeleteGateway(String networkId) {
        log.info("Deleting gateway {}", networkId);
        this.connectionManager.deleteGateway(networkId);
        this.socketSession.getStompSession().send(RESPONSE_SS7_CLIENT_ENDPOINT, String.format("The gateway %s has been deleted", networkId));
    }

    private void handleUpdateErrorCodeMapping(String mnoId) {
        log.info("Updating error code mapping for mno_id {}", mnoId);
        this.connectionManager.updateErrorCodeMapping(mnoId);
        this.socketSession.getStompSession().send(RESPONSE_SS7_CLIENT_ENDPOINT, "The error code mapping has been updated");
    }
}
