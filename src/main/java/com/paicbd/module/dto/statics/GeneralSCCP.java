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
public class GeneralSCCP {
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
