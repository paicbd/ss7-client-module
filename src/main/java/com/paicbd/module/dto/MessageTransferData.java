package com.paicbd.module.dto;

import com.paicbd.module.utils.MessageTransferType;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.utils.Generated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Generated
@AllArgsConstructor
public class MessageTransferData {
    private MessageTransferType messageTransferType;
    private MessageEvent messageEvent;
    private MapRoutingData mapRoutingData;
}
