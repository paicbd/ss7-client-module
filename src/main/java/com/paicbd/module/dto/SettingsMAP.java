package com.paicbd.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsMAP {
    @JsonProperty("id")
    private int id;
    @JsonProperty("network_id")
    private int networkId;
    @JsonProperty("sri_service_op_code")
    private int sriServiceOpCode;
    @JsonProperty("forward_sm_service_op_code")
    private int forwardSmServiceOpCode;
}
