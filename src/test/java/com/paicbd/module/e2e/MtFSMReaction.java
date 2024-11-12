package com.paicbd.module.e2e;

import lombok.Getter;

@Getter
public enum MtFSMReaction {

    RETURN_SUCCESS(0),
    ERROR_UNIDENTIFIED_SUBSCRIBER(5),
    ERROR_ABSENT_SUBSCRIBER_SM(6),
    ERROR_ILLEGAL_SUBSCRIBER(9),
    ERROR_ILLEGAL_EQUIPMENT(12),
    ERROR_FACILITY_NOT_SUPPORTED(21),
    ERROR_ABSENT_SUBSCRIBER(27),
    SUBSCRIBER_BUSY_FOR_MT_SMS(31),
    SM_DELIVERY_FAILURE(32),
    ERROR_SYSTEM_FAILURE(34),
    ERROR_DATA_MISSING(35),
    ERROR_UNEXPECTED_DATA_VALUE(36);

    private final int value;

    MtFSMReaction(int value) {
        this.value = value;
    }
}
