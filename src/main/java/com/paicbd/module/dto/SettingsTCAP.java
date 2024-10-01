package com.paicbd.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingsTCAP {

    @JsonProperty("id")
    private int id;

    @JsonProperty("network_id")
    private int networkId;

    @JsonProperty("ssn_list")
    private String ssnList;

    @JsonProperty("preview_mode")
    private boolean previewMode;

    @JsonProperty("dialog_idle_timeout")
    private int dialogIdleTimeout;

    @JsonProperty("invoke_timeout")
    private int invokeTimeout;

    @JsonProperty("dialog_id_range_start")
    private long dialogIdRangeStart;

    @JsonProperty("dialog_id_range_end")
    private long dialogIdRangeEnd;

    @JsonProperty("max_dialogs")
    private int maxDialogs;

    @JsonProperty("do_not_send_protocol_version")
    private boolean doNotSendProtocolVersion;

    @JsonProperty("swap_tcap_id_enabled")
    private boolean swapTcapIdEnabled;

    @JsonProperty("sls_range_id")
    private String slsRangeId;

    @JsonProperty("exr_delay_thr1")
    private double exrDelayThr1;

    @JsonProperty("exr_delay_thr2")
    private double exrDelayThr2;

    @JsonProperty("exr_delay_thr3")
    private double exrDelayThr3;

    @JsonProperty("exr_back_to_normal_delay_thr1")
    private double exrBackToNormalDelayThr1;

    @JsonProperty("exr_back_to_normal_delay_thr2")
    private double exrBackToNormalDelayThr2;

    @JsonProperty("exr_back_to_normal_delay_thr3")
    private double exrBackToNormalDelayThr3;

    @JsonProperty("memory_monitor_thr1")
    private double memoryMonitorThr1;

    @JsonProperty("memory_monitor_thr2")
    private double memoryMonitorThr2;

    @JsonProperty("memory_monitor_thr3")
    private double memoryMonitorThr3;

    @JsonProperty("mem_back_to_normal_delay_thr1")
    private int memBackToNormalDelayThr1;

    @JsonProperty("mem_back_to_normal_delay_thr2")
    private int memBackToNormalDelayThr2;

    @JsonProperty("mem_back_to_normal_delay_thr3")
    private int memBackToNormalDelayThr3;

    @JsonProperty("blocking_incoming_tcap_messages")
    private boolean blockingIncomingTcapMessages;
}
