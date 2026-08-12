package com.example.demo.matching.admin;

import com.example.demo.matching.model.ReportMatchStatus;
import com.example.demo.report.model.ReportStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One match with everything needed to reproduce its score: the raw inputs, the parameters applied
 * to them, and the resulting points per component.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMatchDetailsDto {

    @NotNull
    private Long id;

    @NotNull
    private Integer score;

    @NotNull
    @Schema(description = "Distance between the two exact locations, the d in the spatial component")
    private Double distanceKm;

    @NotNull
    private Integer distanceScore;

    @NotNull
    private Integer timeGapDays;

    @NotNull
    private Integer timeScore;

    @NotNull
    @Schema(description = "Trigram similarity of title and description, 0 to 1")
    private Double textSimilarity;

    @NotNull
    private Integer textScore;

    @NotNull
    private MatchingConfigDto config;

    @NotNull
    private ReportMatchStatus status;

    @NotBlank
    @Schema(description = "Shared by both listings — matching only pairs within one category")
    private String categoryName;

    @NotNull
    private Long lostReportId;

    @NotBlank
    private String lostReportTitle;

    @NotNull
    private ReportStatus lostReportStatus;

    @NotNull
    private Long foundReportId;

    @NotBlank
    private String foundReportTitle;

    @NotNull
    private ReportStatus foundReportStatus;

    private LocalDateTime lostDismissedAt;

    private LocalDateTime foundDismissedAt;

    private LocalDateTime notifiedAt;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private LocalDateTime updatedAt;
}
