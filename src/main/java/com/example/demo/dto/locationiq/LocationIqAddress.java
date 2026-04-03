package com.example.demo.dto.locationiq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationIqAddress {
    private String country;
    private String city;
    private String road;
    private String name;
    private String suburb;

    @JsonProperty("city_district")
    private String cityDistrict;

    private String neighbourhood;
}
