package com.example.demo.location.dto;

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
    @NotBlank(message = "{validation.location.osmId.required}")
    @Size(max = 50)
    private String osmId;

    @NotBlank(message = "{validation.location.osmType.required}")
    @Size(max = 10)
    private String osmType;
}
