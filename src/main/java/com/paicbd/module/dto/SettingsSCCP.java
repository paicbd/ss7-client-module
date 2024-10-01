package com.paicbd.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SettingsSCCP {

    @JsonProperty("general")
    private General general;

    @JsonProperty("addresses")
    private List<AddressConfig> addresses;

    @JsonProperty("rules")
    private List<RuleConfig> rules;

    @JsonProperty("remote_resources")
    private List<RemoteResourceConfig> remoteResources;

    @JsonProperty("service_access_points")
    private ServiceAccessPointsConfig serviceAccessPoints;

    @Getter
    @Setter
    public static class General {
        @JsonProperty("id")
        private int id;

        @JsonProperty("network_id")
        private int networkId;

        @JsonProperty("z_margin_xudt_message")
        private int zMarginXudtMessage;

        @JsonProperty("remove_spc")
        private boolean removeSpc;

        @JsonProperty("sst_timer_duration_min")
        private int sstTimerDurationMin;

        @JsonProperty("sst_timer_duration_max")
        private int sstTimerDurationMax;

        @JsonProperty("sst_timer_duration_increase_factor")
        private double sstTimerDurationIncreaseFactor;

        @JsonProperty("max_data_message")
        private int maxDataMessage;

        @JsonProperty("period_of_logging")
        private int periodOfLogging;

        @JsonProperty("reassembly_timer_delay")
        private int reassemblyTimerDelay;

        @JsonProperty("preview_mode")
        private boolean previewMode;

        @JsonProperty("sccp_protocol_version")
        private String sccpProtocolVersion;

        @JsonProperty("congestion_control_timer_a")
        private int congestionControlTimerA;

        @JsonProperty("congestion_control_timer_d")
        private int congestionControlTimerD;

        @JsonProperty("congestion_control_algorithm")
        private String congestionControlAlgorithm;

        @JsonProperty("congestion_control")
        private boolean congestionControl;

    }

    @Getter
    @Setter
    public static class AddressConfig {
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

    @Getter
    @Setter
    public static class RuleConfig {
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

    @Getter
    @Setter
    public static class RemoteResourceConfig {
        @JsonProperty("id")
        private int id;

        @JsonProperty("remote_spc")
        private int remoteSpc;

        @JsonProperty("remote_spc_status")
        private String remoteSpcStatus;

        @JsonProperty("remote_sccp_status")
        private String remoteSccpStatus;

        @JsonProperty("remote_ssn")
        private int remoteSsn;

        @JsonProperty("remote_ssn_status")
        private String remoteSsnStatus;

        @JsonProperty("mark_prohibited")
        private boolean markProhibited;

        @JsonProperty("ss7_sccp_id")
        private int ss7SccpId;

    }

    @Getter
    @Setter
    public static class ServiceAccessPointsConfig {
        @JsonProperty("service_access")
        private List<ServiceAccessConfig> serviceAccess;

        @JsonProperty("mtp3_destinations")
        private List<Mtp3DestinationConfig> mtp3Destinations;

    }

    @Getter
    @Setter
    public static class ServiceAccessConfig {
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

    @Getter
    @Setter
    public static class Mtp3DestinationConfig {
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
}
