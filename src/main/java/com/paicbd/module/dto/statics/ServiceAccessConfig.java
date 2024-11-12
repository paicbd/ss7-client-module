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
public class ServiceAccessConfig {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("origin_point_code")
    private int originPointCode;

    @JsonProperty("network_indicator")
    private int networkIndicator;

    @JsonProperty("local_gt_digits")
    private String localGtDigits;

    @JsonProperty("ss7_sccp_id")
    private int ss7SccpId;
}
