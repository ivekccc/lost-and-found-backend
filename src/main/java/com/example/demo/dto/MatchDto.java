package com.example.demo.dto;

import com.example.demo.model.ReportMatchStatus;
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
    private Integer textScore;

    @NotNull
    private ReportMatchStatus status;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private MatchReportSummaryDto myReport;

    @NotNull
    private MatchReportSummaryDto otherReport;
}
