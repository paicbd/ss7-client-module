package com.paicbd.module.e2e;

import lombok.Getter;

@Getter
public enum SRIReaction {

    RETURN_SUCCESS(0),
    ERROR_UNKNOWN_SUBSCRIBER(1),
    ERROR_ABSENT_SUBSCRIBER_SM(6),
    ERROR_TELESERVICE_NOT_PROVISIONED(11),
    ERROR_CALL_BARRED(13),
    ERROR_FACILITY_NOT_SUPPORTED(21),
    ERROR_ABSENT_SUBSCRIBER(27),
    ERROR_SYSTEM_FAILURE(34),
    ERROR_DATA_MISSING(35),
    ERROR_UNEXPECTED_DATA_VALUE(36);

    private final int value;

    SRIReaction(int value) {
        this.value = value;
    }

}
