package com.example.demo.matching.dto;

import com.example.demo.location.model.DistanceBand;
import com.example.demo.matching.model.ReportMatchStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchDto {

    @NotNull
    private Long id;

    /**
     * Match strength 0-100, ROUNDED to the nearest ten. The raw score carries distanceScore at one
     * point per 625 m, so a few crafted listings would trilaterate the other one.
     */
    @NotNull
    @Schema(description = "Match strength 0-100, rounded to the nearest 10 so it cannot be "
            + "used to recover a more precise location than the zone.")
    private Integer score;

    /**
     * Distance band between the two listings' ZONE CENTROIDS, never between exact positions.
     * A band rather than a number because "0.3 km" on a zone of about 1 km² is an artefact of the
     * centroid, accurate to roughly half a kilometre.
     */
    @Schema(description = "How far apart the two reports' zones are, as a band rather than a number. "
            + "Measured between zone centroids, never between exact locations, so a one-decimal figure "
            + "would look far more precise than it is. Absent both when the two reports share the same "
            + "zone (compare the zone names on myReport/otherReport) and when either report's zone is "
            + "unknown.")
    private DistanceBand distanceBand;

    @NotNull
    private Integer timeGapDays;

    @NotNull
    private ReportMatchStatus status;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private MatchReportSummaryDto myReport;

    @NotNull
    private MatchReportSummaryDto otherReport;
}
