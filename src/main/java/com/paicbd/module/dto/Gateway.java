package com.paicbd.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Gateway {
    @JsonProperty("name")
    private String name;

    @JsonProperty("mno_id")
    private Integer mnoId;

    @JsonProperty("network_id")
    private Integer networkId;

    @JsonProperty("m3ua")
    private SettingsM3UA settingsM3UA;

    @JsonProperty("sccp")
    private SettingsSCCP settingsSCCP;

    @JsonProperty("tcap")
    private SettingsTCAP settingsTCAP;

    @JsonProperty("map")
    private SettingsMAP settingsMAP;

}
