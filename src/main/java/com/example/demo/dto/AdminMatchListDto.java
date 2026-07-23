package com.example.demo.dto;

import com.example.demo.model.ReportMatchStatus;
import com.example.demo.model.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMatchListDto {

    @NotNull
    private Long id;

    @NotNull
    private Integer score;

    @NotNull
    private Double distanceKm;

    @NotNull
    private Integer distanceScore;

    @NotNull
    private Integer timeGapDays;

    @NotNull
    private Integer timeScore;

    @NotNull
    private Double textSimilarity;

    @NotNull
    private Integer textScore;

    @NotNull
    private ReportMatchStatus status;

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
