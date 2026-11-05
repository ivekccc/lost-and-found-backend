package com.example.demo.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LocationRequest", description = "Exact point the user confirmed on the map")
public class LocationRequestDTO {

    @NotNull(message = "{validation.location.latitude.required}")
    @DecimalMin(value = "-90", message = "{validation.location.latitude.range}")
    @DecimalMax(value = "90", message = "{validation.location.latitude.range}")
    @Schema(description = "Latitude of the confirmed pin")
    private BigDecimal latitude;

    @NotNull(message = "{validation.location.longitude.required}")
    @DecimalMin(value = "-180", message = "{validation.location.longitude.range}")
    @DecimalMax(value = "180", message = "{validation.location.longitude.range}")
    @Schema(description = "Longitude of the confirmed pin")
    private BigDecimal longitude;
}
