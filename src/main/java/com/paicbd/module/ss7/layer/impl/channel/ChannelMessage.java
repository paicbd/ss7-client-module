package com.paicbd.module.ss7.layer.impl.channel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class ChannelMessage {

    private final String transactionId;
    private final String originId;
    private final Map<String, Object> payloadParameters = new HashMap<>();

    public void setParameter(String name, Object value) {
        payloadParameters.put(name, value);
    }

    public Object getParameter(String name) {
        return payloadParameters.get(name);
    }

    @Override
    public String toString() {
        return String.format("[tid = %s, origin = %s]", transactionId, originId);
    }
}
