package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LocationRequest", description = "Request body for location data")
public class LocationRequestDTO {
    @NotBlank(message = "OSM ID is required")
    @Size(max = 50)
    private String osmId;

    @NotBlank(message = "OSM type is required")
    @Size(max = 10)
    private String osmType;
}
