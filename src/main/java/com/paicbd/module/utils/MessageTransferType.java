package com.paicbd.module.utils;

import com.paicbd.smsc.utils.Generated;
import lombok.Getter;


@Getter
@Generated
public enum MessageTransferType {

    SEND_ROUTING_INFO_FOR_SM("sendRoutingInfoForSM"), SEND_MT_FORWARD_SM("sendMtForwardSM");

    private final String value;

    MessageTransferType(String value) {
        this.value = value;
    }
}
