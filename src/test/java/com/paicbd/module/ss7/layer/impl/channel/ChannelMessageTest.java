package com.paicbd.module.ss7.layer.impl.channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChannelMessageTest {

    private ChannelMessage channelMessage;
    private final String transactionId = "1721677210289-23643471817419";
    private final String originId = "Map";

    @BeforeEach
    void setUp() {
        channelMessage = new ChannelMessage(transactionId, originId);
    }

    @Test
    void testConstructor() {
        assertEquals(transactionId, channelMessage.getTransactionId());
        assertEquals(originId, channelMessage.getOriginId());
    }

    @Test
    void setParameterAdd() {
        String paramName = "key";
        String paramValue = "value";
        channelMessage.setParameter(paramName, paramValue);
        assertEquals(paramValue, channelMessage.getParameter(paramName));
    }

    @Test
    void testSetParameterUpdate() {
        String paramName = "key";
        String paramValue1 = "value1";
        String paramValue2 = "value2";

        channelMessage.setParameter(paramName, paramValue1);
        channelMessage.setParameter(paramName, paramValue2);

        assertEquals(paramValue2, channelMessage.getParameter(paramName));
    }

    @Test
    void testGetParameterNull() {
        String paramName = "nonExistentKey";
        assertNull(channelMessage.getParameter(paramName));
    }


    @Test
    void testToString() {
        String expectedString = "[tid = " + transactionId + ", origin = " + originId + "]";
        assertEquals(expectedString, channelMessage.toString());
    }

    @Test
    void testPayloadParameters() {
        String paramName1 = "key1";
        String paramValue1 = "value1";
        String paramName2 = "key2";
        String paramValue2 = "value2";

        channelMessage.setParameter(paramName1, paramValue1);
        channelMessage.setParameter(paramName2, paramValue2);

        Map<String, Object> payloadParameters = channelMessage.getPayloadParameters();
        assertEquals(2, payloadParameters.size());
        assertEquals(paramValue1, payloadParameters.get(paramName1));
        assertEquals(paramValue2, payloadParameters.get(paramName2));
    }
}