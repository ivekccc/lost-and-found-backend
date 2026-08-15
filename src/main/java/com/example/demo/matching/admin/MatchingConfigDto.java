package com.example.demo.matching.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The parameters the engine is running on right now. Distinct from
 * {@link AppliedMatchingConfigDto}, which is the snapshot a single match was scored with.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MatchingConfigDto", description = "Live scoring parameters of the matching engine")
public class MatchingConfigDto {

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

    @NotNull
    @Schema(description = "When these values were last changed")
    private LocalDateTime updatedAt;
}
