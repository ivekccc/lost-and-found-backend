package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LocationRequest", description = "Request body for location data")
public class LocationRequestDTO {
    @NotNull(message = "Latitude is required")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    private BigDecimal longitude;

    @NotBlank(message = "Display name is required")
    @Size(max = 500)
    private String displayName;

    @Size(max = 50)
    private String placeId;

    @Size(max = 255)
    private String displayPlace;

    @Size(max = 500)
    private String displayAddress;
}
