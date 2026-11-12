package com.example.demo.matching.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "UpdateMatchingConfigRequest",
        description = "New scoring parameters; the three weights must add up to exactly 100")
public class UpdateMatchingConfigRequestDto {

    @NotNull(message = "{validation.matching.maxDistance.required}")
    @DecimalMin(value = "0.1", message = "{validation.matching.maxDistance.positive}")
    @DecimalMax(value = "9999.99", message = "{validation.matching.maxDistance.tooLarge}")
    private BigDecimal maxDistanceKm;

    @NotNull(message = "{validation.matching.weightDistance.required}")
    @Min(value = 0, message = "{validation.matching.weightDistance.range}")
    @Max(value = 100, message = "{validation.matching.weightDistance.range}")
    private Integer weightDistance;

    @NotNull(message = "{validation.matching.weightText.required}")
    @Min(value = 0, message = "{validation.matching.weightText.range}")
    @Max(value = 100, message = "{validation.matching.weightText.range}")
    private Integer weightText;

    @NotNull(message = "{validation.matching.weightTime.required}")
    @Min(value = 0, message = "{validation.matching.weightTime.range}")
    @Max(value = 100, message = "{validation.matching.weightTime.range}")
    private Integer weightTime;

    @NotNull(message = "{validation.matching.timeDecay.required}")
    @Min(value = 1, message = "{validation.matching.timeDecay.min}")
    @Max(value = 3650, message = "{validation.matching.timeDecay.tooLarge}")
    private Integer timeDecayDays;

    @NotNull(message = "{validation.matching.scoreThreshold.required}")
    @Min(value = 0, message = "{validation.matching.scoreThreshold.range}")
    @Max(value = 100, message = "{validation.matching.scoreThreshold.range}")
    private Integer scoreThreshold;

    @NotNull(message = "{validation.matching.trigramShare.required}")
    @DecimalMin(value = "0", message = "{validation.matching.trigramShare.range}")
    @DecimalMax(value = "1", message = "{validation.matching.trigramShare.range}")
    @Digits(integer = 1, fraction = 2, message = "{validation.matching.twoDecimals}")
    private BigDecimal trigramShare;

    @NotNull(message = "{validation.matching.semanticFloor.required}")
    @DecimalMin(value = "0", message = "{validation.matching.semanticFloor.range}")
    @DecimalMax(value = "0.95", message = "{validation.matching.semanticFloor.range}")
    @Digits(integer = 1, fraction = 2, message = "{validation.matching.twoDecimals}")
    private BigDecimal semanticFloor;

    /**
     * A total score of 100 is what makes the stored score readable as a percentage, so the weights
     * are constrained rather than normalised — silently rescaling them would make the admin's
     * numbers disagree with the ones on the match screen.
     */
    @AssertTrue(message = "{validation.matching.weightsSum}")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isWeightsSumValid() {
        return weightDistance == null || weightText == null || weightTime == null
                || weightDistance + weightText + weightTime == 100;
    }
}
