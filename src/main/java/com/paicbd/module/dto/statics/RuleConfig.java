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
public class RuleConfig {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("mask")
    private String mask;

    @JsonProperty("address_indicator")
    private int calledAddressIndicator;

    @JsonProperty("point_code")
    private int calledPointCode;

    @JsonProperty("subsystem_number")
    private int calledSubsystemNumber;

    @JsonProperty("gt_indicator")
    private String calledGtIndicator;

    @JsonProperty("translation_type")
    private int calledTranslationType;

    @JsonProperty("numbering_plan_id")
    private int calledNumberingPlanId;

    @JsonProperty("nature_of_address_id")
    private int calledNatureOfAddressId;

    @JsonProperty("global_tittle_digits")
    private String calledGlobalTittleDigits;

    @JsonProperty("rule_type_id")
    private int ruleTypeId;

    @JsonProperty("primary_address_id")
    private Integer primaryAddressId;

    @JsonProperty("secondary_address_id")
    private Integer secondaryAddressId;

    @JsonProperty("load_sharing_algorithm_id")
    private int loadSharingAlgorithmId;

    @JsonProperty("new_calling_party_address")
    private String newCallingPartyAddress;

    @JsonProperty("origination_type_id")
    private int originationTypeId;

    @JsonProperty("calling_address_indicator")
    private int callingAddressIndicator;

    @JsonProperty("calling_point_code")
    private int callingPointCode;

    @JsonProperty("calling_subsystem_number")
    private int callingSubsystemNumber;

    @JsonProperty("calling_translator_type")
    private int callingTranslationType;

    @JsonProperty("calling_numbering_plan_id")
    private int callingNumberingPlanId;

    @JsonProperty("calling_nature_of_address_id")
    private int callingNatureOfAddressId;

    @JsonProperty("calling_gt_indicator")
    private String callingGtIndicator;

    @JsonProperty("calling_global_tittle_digits")
    private String callingGlobalTittleDigits;
}
