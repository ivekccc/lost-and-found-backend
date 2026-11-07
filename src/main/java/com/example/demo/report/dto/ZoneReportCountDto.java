package com.example.demo.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ZoneReportCountDto", description = "How many matching listings sit in one zone, placed at the "
        + "zone's centroid. Never an exact location.")
public class ZoneReportCountDto {

    @NotNull
    private Long zoneId;

    @NotBlank
    private String zoneName;

    @Schema(description = "Municipality the zone sits in; null for a municipality itself.")
    private Long parentId;

    @Schema(description = "Name of the parent municipality; null when it repeats the zone's own name.")
    private String parentName;

    @NotNull
    private BigDecimal latitude;

    @NotNull
    private BigDecimal longitude;

    @NotNull
    private Long lostCount;

    @NotNull
    private Long foundCount;
}
