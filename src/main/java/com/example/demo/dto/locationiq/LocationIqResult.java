package com.example.demo.dto.locationiq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationIqResult {
    @JsonProperty("osm_id")
    private String osmId;

    @JsonProperty("osm_type")
    private String osmType;

    private String lat;
    private String lon;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("display_place")
    private String displayPlace;

    @JsonProperty("display_address")
    private String displayAddress;

    private LocationIqAddress address;
}
