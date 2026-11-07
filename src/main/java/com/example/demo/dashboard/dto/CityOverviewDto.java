package com.example.demo.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "CityOverviewDto", description = "Listings per city: active now, and reunited within the requested period.")
public record CityOverviewDto(
        @NotNull Long cityId,
        @NotBlank String cityName,
        @NotNull long activeReports,
        @NotNull long reunitedReports) {
}
