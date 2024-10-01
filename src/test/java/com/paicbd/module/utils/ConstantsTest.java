package com.paicbd.module.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConstantsTest {

    @Test
    void testPrivateConstructor() throws NoSuchMethodException {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    void testSocketConstantsValues() {
        assertEquals("/app/updateErrorCodeMapping", Constants.UPDATE_ERROR_CODE_MAPPING_ENDPOINT);
        assertEquals("/app/ss7/deleteGateway", Constants.DELETE_SS7_GATEWAY_ENDPOINT);
        assertEquals("/app/ss7/updateGateway", Constants.UPDATE_SS7_GATEWAY_ENDPOINT);
        assertEquals("/app/response-ss7-client", Constants.RESPONSE_SS7_CLIENT_ENDPOINT);
        assertEquals("SS7", Constants.PROTOCOL);
        assertEquals("GW", Constants.GW);
    }

    @Test
    void testSS7ConstantsValues() {
        assertEquals("_absent_subscriber", Constants.ABSENT_SUBSCRIBER_HASH_NAME);
        assertEquals("messageType", Constants.MESSAGE_TYPE);
        assertEquals("message", Constants.MESSAGE);
        assertEquals("invokeId", Constants.INVOKE_ID);
        assertEquals("dialog", Constants.DIALOG);
        assertEquals("mapErrorMessage", Constants.MAP_ERROR_MESSAGE);
        assertEquals("onErrorComponent", Constants.ON_ERROR_COMPONENT);
        assertEquals("onRejectComponent", Constants.ON_REJECT_COMPONENT);
        assertEquals("onInvokeTimeout", Constants.ON_INVOKE_TIMEOUT);
        assertEquals("onMapMessage", Constants.ON_MAP_MESSAGE);
        assertEquals("mapMessage", Constants.MAP_MESSAGE);
        assertEquals("problem", Constants.PROBLEM);
        assertEquals("localOriginated", Constants.LOCAL_ORIGINATED);
        assertEquals("mapExtensionContainer", Constants.MAP_EXTENSION_CONTAINER);
        assertEquals("onDialogClose", Constants.ON_DIALOG_CLOSE);
        assertEquals("onDialogTimeout", Constants.ON_DIALOG_TIMEOUT);
        assertEquals("TCAP_EVENT_TYPE", Constants.TCAP_MESSAGE_TYPE);
        assertEquals("TCAP_MESSAGE", Constants.TCAP_MESSAGE);
        assertEquals("TCAP_MESSAGE_DIALOG", Constants.TCAP_MESSAGE_DIALOG);
    }

}