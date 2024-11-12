package com.paicbd.module.components;

import com.paicbd.module.ss7.ConnectionManager;
import com.paicbd.smsc.ws.FrameHandler;
import com.paicbd.smsc.ws.SocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static com.paicbd.module.utils.Constants.CONNECT_SS7_GATEWAY_ENDPOINT;
import static com.paicbd.module.utils.Constants.RESPONSE_SS7_CLIENT_ENDPOINT;
import static com.paicbd.module.utils.Constants.DELETE_SS7_GATEWAY_ENDPOINT;
import static com.paicbd.module.utils.Constants.START_SS7_ASSOCIATION_ENDPOINT;
import static com.paicbd.module.utils.Constants.START_SS7_SOCKET_ENDPOINT;
import static com.paicbd.module.utils.Constants.STOP_SS7_ASSOCIATION_ENDPOINT;
import static com.paicbd.module.utils.Constants.STOP_SS7_GATEWAY_ENDPOINT;
import static com.paicbd.module.utils.Constants.STOP_SS7_SOCKET_ENDPOINT;
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
        Objects.requireNonNull(payloadId, "payload cannot be null");
        Objects.requireNonNull(destination, "Destination cannot be null");

        switch (destination) {
            case UPDATE_SS7_GATEWAY_ENDPOINT, CONNECT_SS7_GATEWAY_ENDPOINT -> handleUpdateGateway(payloadId);
            case DELETE_SS7_GATEWAY_ENDPOINT, STOP_SS7_GATEWAY_ENDPOINT -> handleDeleteGateway(payloadId);
            case START_SS7_ASSOCIATION_ENDPOINT -> handleStartAssociation(payloadId);
            case STOP_SS7_ASSOCIATION_ENDPOINT -> handleStopAssociation(payloadId);
            case START_SS7_SOCKET_ENDPOINT -> handleStartSocket(payloadId);
            case STOP_SS7_SOCKET_ENDPOINT -> handleStopSocket(payloadId);
            // Payload must be mno_id
            case UPDATE_ERROR_CODE_MAPPING_ENDPOINT -> handleUpdateErrorCodeMapping(payloadId);
            default -> log.warn("Received notification for unknown destination {}", destination);
        }
    }

    private void handleUpdateGateway(String networkId) {
        try {
            Integer.parseInt(networkId);
            log.info("Updating gateway {}", networkId);
            this.connectionManager.updateGateway(networkId);
            this.socketSession.getStompSession().send(RESPONSE_SS7_CLIENT_ENDPOINT, "The gateway has been updated");
        } catch (NumberFormatException e) {
            log.error("Error on updating gateway {} is not a number", networkId);
        }
    }

    private void handleDeleteGateway(String networkId) {
        try {
            Integer.parseInt(networkId);
            log.info("Deleting gateway {}", networkId);
            this.connectionManager.deleteGateway(networkId);
            this.socketSession.getStompSession().send(RESPONSE_SS7_CLIENT_ENDPOINT, String.format("The gateway %s has been deleted", networkId));
        } catch (NumberFormatException e) {
            log.error("Error on delete gateway {} is not a number", networkId);
        }

    }

    private void handleUpdateErrorCodeMapping(String mnoId) {
        try {
            Integer.parseInt(mnoId);
            log.info("Updating error code mapping for mno_id {}", mnoId);
            this.connectionManager.updateErrorCodeMapping(mnoId);
            this.socketSession.getStompSession().send(RESPONSE_SS7_CLIENT_ENDPOINT, "The error code mapping has been updated");
        } catch (NumberFormatException e) {
            log.error("Error on update error code mapping {} is not a number", mnoId);
        }
    }

    private void handleStopAssociation(String payload) {
        var payloadBody = this.getNetworkIdAndData(payload);
        String networkId = payloadBody[0];
        String associationName = payloadBody[1];
        log.info("Stopping association for association {} in  networkId {}", associationName, networkId);
        this.connectionManager.manageAssociation(networkId, associationName, false);
        this.socketSession.getStompSession().send(RESPONSE_SS7_CLIENT_ENDPOINT, "The association has been stopped");
    }

    private void handleStartAssociation(String payload) {
        var payloadBody = this.getNetworkIdAndData(payload);
        String networkId = payloadBody[0];
        String associationName = payloadBody[1];
        log.info("Starting association for association {} in  networkId {}", associationName, networkId);
        this.connectionManager.manageAssociation(networkId, associationName, true);
        this.socketSession.getStompSession().send(RESPONSE_SS7_CLIENT_ENDPOINT, "The association has been started");
    }

    private void handleStopSocket(String payload) {
        log.info("Stopping Socket");
        var payloadBody = this.getNetworkIdAndData(payload);
        String networkId = payloadBody[0];
        String socketId = payloadBody[1];
        this.connectionManager.manageSocket(networkId, Integer.parseInt(socketId), false);
        this.socketSession.getStompSession().send(RESPONSE_SS7_CLIENT_ENDPOINT, "The socket has been stopped");
    }

    private void handleStartSocket(String payload) {
        log.info("Starting Socket");
        var payloadBody = this.getNetworkIdAndData(payload);
        String networkId = payloadBody[0];
        String socketId = payloadBody[1];
        this.connectionManager.manageSocket(networkId, Integer.parseInt(socketId), true);
        this.socketSession.getStompSession().send(RESPONSE_SS7_CLIENT_ENDPOINT, "The socket has been started");
    }

    private String[] getNetworkIdAndData(String payload) {
        var payloadBody = payload.split("-");
        if (payloadBody.length == 2) {
            return payloadBody;
        } else {
            var payloadAsList = new ArrayList<>(Arrays.stream(payloadBody).toList()) ;
            String networkId = payloadAsList.removeFirst();
            String associationName = String.join("-", payloadAsList);
            return new String[]{networkId, associationName};
        }
    }
}
