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
public class AddressConfig {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("digits")
    private String digits;

    @JsonProperty("address_indicator")
    private int addressIndicator;

    @JsonProperty("point_code")
    private int pointCode;

    @JsonProperty("subsystem_number")
    private int subsystemNumber;

    @JsonProperty("gt_indicator")
    private String gtIndicator;

    @JsonProperty("translation_type")
    private int translationType;

    @JsonProperty("numbering_plan_id")
    private int numberingPlanId;

    @JsonProperty("nature_of_address_id")
    private int natureOfAddressId;

    @JsonProperty("ss7_sccp_id")
    private int ss7SccpId;
}
