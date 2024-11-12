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
public class RemoteResourceConfig {
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
