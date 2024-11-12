package com.paicbd.module.ss7.layer.impl.channel;

import com.paicbd.module.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.restcomm.protocols.ss7.map.api.service.sms.MoForwardShortMessageRequest;
import org.restcomm.protocols.ss7.map.service.sms.MoForwardShortMessageRequestImpl;
import org.restcomm.protocols.ss7.map.service.sms.SendRoutingInfoForSMRequestImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelMessageTest {

    private ChannelMessage channelMessage;
    private final String transactionId = "1721677210289-23643471817419";
    private final String originId = "Map";

    @BeforeEach
    void setUp() {
        channelMessage = new ChannelMessage(transactionId, originId);
    }

    @Test
    @DisplayName("Add parameter to the payload parameters map")
    void setParameterWhenAddParamsThenGet() {
        String paramName = Constants.MESSAGE_TYPE;
        MoForwardShortMessageRequest moForwardShortMessageRequest = new MoForwardShortMessageRequestImpl();
        String paramValue = moForwardShortMessageRequest.getMessageType().toString();
        channelMessage.setParameter(paramName, paramValue);
        assertEquals(paramValue, channelMessage.getParameter(paramName));
    }

    @Test
    @DisplayName("Update parameter in the payload parameters map")
    void setParameterWhenUpdateParamsThenGet() {
        String paramName = Constants.MESSAGE_TYPE;
        MoForwardShortMessageRequest moForwardShortMessageRequest = new MoForwardShortMessageRequestImpl();
        SendRoutingInfoForSMRequestImpl sendRoutingInfoForSMRequest = new SendRoutingInfoForSMRequestImpl();
        String paramValue = moForwardShortMessageRequest.getMessageType().toString();
        String paramValueUpdated = sendRoutingInfoForSMRequest.getMessageType().toString();

        channelMessage.setParameter(paramName, paramValue);
        assertEquals(paramValue, channelMessage.getParameter(paramName));

        channelMessage.setParameter(paramName, paramValueUpdated);
        assertEquals(paramValueUpdated, channelMessage.getParameter(paramName));
    }

    @Test
    @DisplayName("Test toString method")
    void toStringWhenAllParamsAreValid() {
        String expectedString = "[tid = " + transactionId + ", origin = " + originId + "]";
        assertEquals(expectedString, channelMessage.toString());
    }
}