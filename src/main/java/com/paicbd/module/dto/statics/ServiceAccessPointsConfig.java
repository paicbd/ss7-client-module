package com.paicbd.module.dto.statics;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ServiceAccessPointsConfig {
    @JsonProperty("service_access")
    private List<ServiceAccessConfig> serviceAccess;

    @JsonProperty("mtp3_destinations")
    private List<Mtp3DestinationConfig> mtp3Destinations;
}
