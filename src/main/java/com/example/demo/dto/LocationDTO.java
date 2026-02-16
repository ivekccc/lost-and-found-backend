package com.example.demo.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Location data")
public class LocationDTO {
    private Long id;
    private Double latitude;
    private Double longitude;
    private String formattedAddress;
    private String osmId;
    private String country;
    private String city;
    private String district;
    private String street;
}
