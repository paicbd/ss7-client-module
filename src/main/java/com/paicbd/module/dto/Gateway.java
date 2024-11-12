package com.paicbd.module.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Generated
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gateway {
    @JsonProperty("name")
    private String name;

    private int enabled;

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

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }
}
