package com.paicbd.module.components;

import com.paicbd.module.ss7.ConnectionManager;
import com.paicbd.smsc.ws.SocketSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;

import static com.paicbd.module.utils.Constants.CONNECT_SS7_GATEWAY_ENDPOINT;
import static com.paicbd.module.utils.Constants.DELETE_SS7_GATEWAY_ENDPOINT;
import static com.paicbd.module.utils.Constants.RESPONSE_SS7_CLIENT_ENDPOINT;
import static com.paicbd.module.utils.Constants.START_SS7_ASSOCIATION_ENDPOINT;
import static com.paicbd.module.utils.Constants.START_SS7_SOCKET_ENDPOINT;
import static com.paicbd.module.utils.Constants.STOP_SS7_ASSOCIATION_ENDPOINT;
import static com.paicbd.module.utils.Constants.STOP_SS7_GATEWAY_ENDPOINT;
import static com.paicbd.module.utils.Constants.STOP_SS7_SOCKET_ENDPOINT;
import static com.paicbd.module.utils.Constants.UPDATE_ERROR_CODE_MAPPING_ENDPOINT;
import static com.paicbd.module.utils.Constants.UPDATE_SS7_GATEWAY_ENDPOINT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CustomFrameHandlerTest {

    @Mock
    private SocketSession socketSession;

    @Mock
    private StompHeaders stompHeaders;

    @Mock
    private StompSession stompSession;

    @Mock
    private ConnectionManager connectionManager;

    @InjectMocks
    CustomFrameHandler customFrameHandler;


    @Test
    @DisplayName("HandleFrameLogic when update gateway then send response")
    void handleFrameLogicWhenUpdateGatewayThenSendResponse() {
        //NetworkId of the GW
        String payload = "4";
        when(socketSession.getStompSession()).thenReturn(stompSession);
        when(stompHeaders.getDestination()).thenReturn(UPDATE_SS7_GATEWAY_ENDPOINT);
        customFrameHandler.handleFrameLogic(stompHeaders, payload);
        verify(connectionManager).updateGateway(payload);
        verify(stompSession).send(RESPONSE_SS7_CLIENT_ENDPOINT, "The gateway has been updated");
    }

    @Test
    @DisplayName("HandleFrameLogic when connect gateway then send response")
    void handleFrameLogicWhenConnectGatewayThenSendResponse() {
        //NetworkId of the GW
        String payload = "4";
        when(socketSession.getStompSession()).thenReturn(stompSession);
        when(stompHeaders.getDestination()).thenReturn(CONNECT_SS7_GATEWAY_ENDPOINT);
        customFrameHandler.handleFrameLogic(stompHeaders, payload);
        verify(connectionManager).updateGateway(payload);
        verify(stompSession).send(RESPONSE_SS7_CLIENT_ENDPOINT, "The gateway has been updated");
    }


    @Test
    @DisplayName("HandleFrameLogic when delete gateway then send response")
    void handleFrameLogicWhenDeleteGatewayThenSendResponse() {
        //NetworkId of the GW
        String payload = "4";
        when(socketSession.getStompSession()).thenReturn(stompSession);
        when(stompHeaders.getDestination()).thenReturn(DELETE_SS7_GATEWAY_ENDPOINT);
        customFrameHandler.handleFrameLogic(stompHeaders, payload);
        verify(connectionManager).deleteGateway(payload);
        verify(stompSession).send(RESPONSE_SS7_CLIENT_ENDPOINT, String.format("The gateway %s has been deleted", payload));
    }

    @Test
    @DisplayName("HandleFrameLogic when stop gateway then send response")
    void handleFrameLogicWhenStopGatewayThenSendResponse() {
        //NetworkId of the GW
        String payload = "4";
        when(socketSession.getStompSession()).thenReturn(stompSession);
        when(stompHeaders.getDestination()).thenReturn(STOP_SS7_GATEWAY_ENDPOINT);
        customFrameHandler.handleFrameLogic(stompHeaders, payload);
        verify(connectionManager).deleteGateway(payload);
        verify(stompSession).send(RESPONSE_SS7_CLIENT_ENDPOINT, String.format("The gateway %s has been deleted", payload));
    }

    @Test
    @DisplayName("HandleFrameLogic when update error code mapping then send response")
    void handleFrameLogicWhenUpdateErrorCodeMappingThenSendResponse() {
        //MnoId
        String payload = "1";
        when(socketSession.getStompSession()).thenReturn(stompSession);
        when(stompHeaders.getDestination()).thenReturn(UPDATE_ERROR_CODE_MAPPING_ENDPOINT);
        customFrameHandler.handleFrameLogic(stompHeaders, payload);
        verify(connectionManager).updateErrorCodeMapping(payload);
        verify(stompSession).send(RESPONSE_SS7_CLIENT_ENDPOINT, "The error code mapping has been updated");
    }


    @Test
    @DisplayName("HandleFrameLogic when start association with single name then send response")
    void handleFrameLogicWhenStartAssociationSingleThenSendResponse() {
        //networkId-assocName
        String payloadSingle = "1-assoc";
        String networkId = "1";
        String assocName = "assoc";

        ArgumentCaptor<String> networkIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> assocNameCaptor = ArgumentCaptor.forClass(String.class);
        when(socketSession.getStompSession()).thenReturn(stompSession);
        when(stompHeaders.getDestination()).thenReturn(START_SS7_ASSOCIATION_ENDPOINT);
        customFrameHandler.handleFrameLogic(stompHeaders, payloadSingle);
        verify(connectionManager).manageAssociation(networkIdCaptor.capture(), assocNameCaptor.capture(), eq(true));
        Assertions.assertEquals(networkId, networkIdCaptor.getValue());
        Assertions.assertEquals(assocName, assocNameCaptor.getValue());
        verify(stompSession).send(RESPONSE_SS7_CLIENT_ENDPOINT, "The association has been started");
    }

    @Test
    @DisplayName("HandleFrameLogic when start association with multi name then send response")
    void handleFrameLogicWhenStartAssociationMultiThenSendResponse() {
        //networkId-assocName
        String payloadMulti = "1-assoc-name";
        String networkId = "1";
        String assocName = "assoc-name";

        ArgumentCaptor<String> networkIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> assocNameCaptor = ArgumentCaptor.forClass(String.class);
        when(socketSession.getStompSession()).thenReturn(stompSession);
        when(stompHeaders.getDestination()).thenReturn(START_SS7_ASSOCIATION_ENDPOINT);
        customFrameHandler.handleFrameLogic(stompHeaders, payloadMulti);
        verify(connectionManager).manageAssociation(networkIdCaptor.capture(), assocNameCaptor.capture(), eq(true));
        Assertions.assertEquals(networkId, networkIdCaptor.getValue());
        Assertions.assertEquals(assocName, assocNameCaptor.getValue());
        verify(stompSession).send(RESPONSE_SS7_CLIENT_ENDPOINT, "The association has been started");
    }


    @Test
    @DisplayName("HandleFrameLogic when stop association with single name then send response")
    void handleFrameLogicWhenStopAssociationSingleThenSendResponse() {
        //networkId-assocName
        String payloadSingle = "1-assoc";
        String networkId = "1";
        String assocName = "assoc";

        ArgumentCaptor<String> networkIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> assocNameCaptor = ArgumentCaptor.forClass(String.class);
        when(socketSession.getStompSession()).thenReturn(stompSession);
        when(stompHeaders.getDestination()).thenReturn(STOP_SS7_ASSOCIATION_ENDPOINT);
        customFrameHandler.handleFrameLogic(stompHeaders, payloadSingle);
        verify(connectionManager).manageAssociation(networkIdCaptor.capture(), assocNameCaptor.capture(), eq(false));
        Assertions.assertEquals(networkId, networkIdCaptor.getValue());
        Assertions.assertEquals(assocName, assocNameCaptor.getValue());
        verify(stompSession).send(RESPONSE_SS7_CLIENT_ENDPOINT, "The association has been stopped");
    }

    @Test
    @DisplayName("HandleFrameLogic when stop association with multi name then send response")
    void handleFrameLogicWhenStopAssociationMultiThenSendResponse() {
        //networkId-assocName
        String payloadMulti = "1-assoc-name";
        String networkId = "1";
        String assocName = "assoc-name";

        ArgumentCaptor<String> networkIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> assocNameCaptor = ArgumentCaptor.forClass(String.class);
        when(socketSession.getStompSession()).thenReturn(stompSession);
        when(stompHeaders.getDestination()).thenReturn(STOP_SS7_ASSOCIATION_ENDPOINT);
        customFrameHandler.handleFrameLogic(stompHeaders, payloadMulti);
        verify(connectionManager).manageAssociation(networkIdCaptor.capture(), assocNameCaptor.capture(), eq(false));
        Assertions.assertEquals(networkId, networkIdCaptor.getValue());
        Assertions.assertEquals(assocName, assocNameCaptor.getValue());
        verify(stompSession).send(RESPONSE_SS7_CLIENT_ENDPOINT, "The association has been stopped");
    }

    @Test
    @DisplayName("HandleFrameLogic when start socket then send response")
    void handleFrameLogicWhenStartSocketThenSendResponse() {
        //networkId-socketId
        String payload = "1-1";
        String networkId = "1";
        int socketId = 1;

        ArgumentCaptor<String> networkIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> socketIdCaptor = ArgumentCaptor.forClass(Integer.class);
        when(socketSession.getStompSession()).thenReturn(stompSession);
        when(stompHeaders.getDestination()).thenReturn(START_SS7_SOCKET_ENDPOINT);
        customFrameHandler.handleFrameLogic(stompHeaders, payload);
        verify(connectionManager).manageSocket(networkIdCaptor.capture(), socketIdCaptor.capture(), eq(true));
        Assertions.assertEquals(networkId, networkIdCaptor.getValue());
        Assertions.assertEquals(socketId, socketIdCaptor.getValue());
        verify(stompSession).send(RESPONSE_SS7_CLIENT_ENDPOINT, "The socket has been started");
    }

    @Test
    @DisplayName("HandleFrameLogic when stop socket then send response")
    void handleFrameLogicWhenStopSocketThenSendResponse() {
        //networkId-assocName
        String payload = "1-10";
        String networkId = "1";
        int socketId = 10;

        ArgumentCaptor<String> networkIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> socketIdCaptor = ArgumentCaptor.forClass(Integer.class);
        when(socketSession.getStompSession()).thenReturn(stompSession);
        when(stompHeaders.getDestination()).thenReturn(STOP_SS7_SOCKET_ENDPOINT);
        customFrameHandler.handleFrameLogic(stompHeaders, payload);
        verify(connectionManager).manageSocket(networkIdCaptor.capture(), socketIdCaptor.capture(), eq(false));
        Assertions.assertEquals(networkId, networkIdCaptor.getValue());
        Assertions.assertEquals(socketId, socketIdCaptor.getValue());
        verify(stompSession).send(RESPONSE_SS7_CLIENT_ENDPOINT, "The socket has been stopped");
    }

    @Test
    @DisplayName("HandleFrameLogic when invalid destination then don't execute action")
    void handleFrameLogicWhenInvalidDestinationThenDontExecuteAction() {
        String payload = "smpp";
        when(stompHeaders.getDestination()).thenReturn("INVALID_DESTINATION");
        customFrameHandler.handleFrameLogic(stompHeaders, payload);
        verifyNoInteractions(connectionManager);
        verifyNoInteractions(stompSession);
    }

    @ParameterizedTest
    @DisplayName("HandleFrameLogic when payload is not a number then don't execute action")
    @ValueSource(strings = {UPDATE_SS7_GATEWAY_ENDPOINT, CONNECT_SS7_GATEWAY_ENDPOINT, STOP_SS7_GATEWAY_ENDPOINT, DELETE_SS7_GATEWAY_ENDPOINT,
            UPDATE_ERROR_CODE_MAPPING_ENDPOINT})
    void handleFrameLogicKeyWhenPayloadIsNotNumberThenDontExecuteAction(String endpoint) {
        when(stompHeaders.getDestination()).thenReturn(endpoint);
        String payload = "systemId";
        customFrameHandler.handleFrameLogic(stompHeaders, payload);
        verify(connectionManager, never()).updateGateway(payload);
        verify(connectionManager,  never()).deleteGateway(payload);
        verify(connectionManager,  never()).updateErrorCodeMapping(payload);
    }
}