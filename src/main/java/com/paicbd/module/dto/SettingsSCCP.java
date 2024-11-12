package com.paicbd.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.module.dto.statics.AddressConfig;
import com.paicbd.module.dto.statics.GeneralSCCP;
import com.paicbd.module.dto.statics.RemoteResourceConfig;
import com.paicbd.module.dto.statics.RuleConfig;
import com.paicbd.module.dto.statics.ServiceAccessPointsConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsSCCP {

    @JsonProperty("general")
    private GeneralSCCP generalSCCP;

    @JsonProperty("addresses")
    private List<AddressConfig> addresses;

    @JsonProperty("rules")
    private List<RuleConfig> rules;

    @JsonProperty("remote_resources")
    private List<RemoteResourceConfig> remoteResources;

    @JsonProperty("service_access_points")
    private ServiceAccessPointsConfig serviceAccessPoints;
}
