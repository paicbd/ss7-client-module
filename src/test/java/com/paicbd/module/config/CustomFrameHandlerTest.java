package com.paicbd.module.config;

import com.paicbd.module.ss7.ConnectionManager;
import com.paicbd.smsc.ws.SocketSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;

import static com.paicbd.module.utils.Constants.DELETE_SS7_GATEWAY_ENDPOINT;
import static com.paicbd.module.utils.Constants.UPDATE_ERROR_CODE_MAPPING_ENDPOINT;
import static com.paicbd.module.utils.Constants.UPDATE_SS7_GATEWAY_ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CustomFrameHandlerTest {

    @Mock(strictness = Mock.Strictness.LENIENT) // In handleFrameLogic_invalidDestination test case, IllegalArgumentException is thrown by the method
    private SocketSession socketSession;

    @Mock
    private StompHeaders stompHeaders;

    @Mock
    private StompSession stompSession;

    @Mock
    private ConnectionManager connectionManager;

    @InjectMocks
    CustomFrameHandler customFrameHandler;

    @BeforeEach
    void setUp() {
        when(socketSession.getStompSession()).thenReturn(stompSession);
    }

    @Test
    void handleFrameLogic_updateGateway() {
        //NetworkId of the GW
        String payload = "4";
        when(stompHeaders.getDestination()).thenReturn(UPDATE_SS7_GATEWAY_ENDPOINT);
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, payload));
    }

    @Test
    void handleFrameLogic_deleteGateway() {
        //NetworkId of the GW
        String payload = "4";
        when(stompHeaders.getDestination()).thenReturn(DELETE_SS7_GATEWAY_ENDPOINT);
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, payload));
    }

    @Test
    void handleFrameLogic_updateErrorCodeMapping() {
        //MnoId
        String payload = "1";
        when(stompHeaders.getDestination()).thenReturn(UPDATE_ERROR_CODE_MAPPING_ENDPOINT);
        assertDoesNotThrow(() -> customFrameHandler.handleFrameLogic(stompHeaders, payload));
    }

    @Test
    void handleFrameLogic_invalidDestination() {
        String payload = "smpp";
        when(stompHeaders.getDestination()).thenReturn("INVALID_DESTINATION");
        assertDoesNotThrow(() -> {
            customFrameHandler.handleFrameLogic(stompHeaders, payload);
        });
    }

    @Test
    void handleFrameLogic_payloadNull() {
        assertThrows(NullPointerException.class, () -> {
            customFrameHandler.handleFrameLogic(stompHeaders, null);
        });
    }
}