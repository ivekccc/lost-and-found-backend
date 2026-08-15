package com.example.demo.matching.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * The scoring parameters a match was computed with, read from the match row rather than from the
 * live configuration — so the breakdown stays reproducible after the engine is retuned.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AppliedMatchingConfigDto", description = "Matching parameters as applied when this match was computed")
public class AppliedMatchingConfigDto {

    @NotNull
    @Schema(description = "Hard cut-off; candidates further apart are dropped before scoring")
    private BigDecimal maxDistanceKm;

    @NotNull
    private Integer weightDistance;

    @NotNull
    private Integer weightText;

    @NotNull
    private Integer weightTime;

    @NotNull
    @Schema(description = "Days after which the time signal contributes nothing")
    private Integer timeDecayDays;

    @NotNull
    @Schema(description = "Minimum total score for a pair to be suggested")
    private Integer scoreThreshold;
}
