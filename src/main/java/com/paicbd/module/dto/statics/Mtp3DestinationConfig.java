package com.paicbd.module.dto.statics;

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
public class Mtp3DestinationConfig {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("first_point_code")
    private int firstPointCode;

    @JsonProperty("last_point_code")
    private int lastPointCode;

    @JsonProperty("first_sls")
    private int firstSls;

    @JsonProperty("last_sls")
    private int lastSls;

    @JsonProperty("sls_mask")
    private int slsMask;

    @JsonProperty("sccp_sap_id")
    private int sccpSapId;
}
