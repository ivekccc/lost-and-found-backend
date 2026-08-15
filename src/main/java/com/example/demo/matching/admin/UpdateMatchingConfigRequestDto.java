package com.example.demo.matching.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "UpdateMatchingConfigRequest",
        description = "New scoring parameters; the three weights must add up to exactly 100")
public class UpdateMatchingConfigRequestDto {

    @NotNull(message = "Max distance is required")
    @DecimalMin(value = "0.1", message = "Max distance must be greater than zero")
    @DecimalMax(value = "9999.99", message = "Max distance is too large")
    private BigDecimal maxDistanceKm;

    @NotNull(message = "Distance weight is required")
    @Min(value = 0, message = "Distance weight must be between 0 and 100")
    @Max(value = 100, message = "Distance weight must be between 0 and 100")
    private Integer weightDistance;

    @NotNull(message = "Text weight is required")
    @Min(value = 0, message = "Text weight must be between 0 and 100")
    @Max(value = 100, message = "Text weight must be between 0 and 100")
    private Integer weightText;

    @NotNull(message = "Time weight is required")
    @Min(value = 0, message = "Time weight must be between 0 and 100")
    @Max(value = 100, message = "Time weight must be between 0 and 100")
    private Integer weightTime;

    @NotNull(message = "Time decay is required")
    @Min(value = 1, message = "Time decay must be at least one day")
    @Max(value = 3650, message = "Time decay is too large")
    private Integer timeDecayDays;

    @NotNull(message = "Score threshold is required")
    @Min(value = 0, message = "Score threshold must be between 0 and 100")
    @Max(value = 100, message = "Score threshold must be between 0 and 100")
    private Integer scoreThreshold;

    /**
     * A total score of 100 is what makes the stored score readable as a percentage, so the weights
     * are constrained rather than normalised — silently rescaling them would make the admin's
     * numbers disagree with the ones on the match screen.
     */
    @AssertTrue(message = "Weights must add up to exactly 100")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isWeightsSumValid() {
        return weightDistance == null || weightText == null || weightTime == null
                || weightDistance + weightText + weightTime == 100;
    }
}
