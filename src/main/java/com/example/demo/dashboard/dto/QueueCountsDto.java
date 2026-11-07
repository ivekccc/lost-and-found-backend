package com.example.demo.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(name = "QueueCountsDto", description = "Work waiting for a moderator right now.")
public record QueueCountsDto(
        @NotNull long pendingAbuseReports,
        @Schema(description = "When the oldest pending abuse report was filed; null when the queue is empty.")
        LocalDateTime oldestPendingAbuseReportAt,
        @NotNull long flaggedReports,
        @NotNull long pendingClaims,
        @Schema(description = "When the oldest pending ownership claim was submitted; null when there is none.")
        LocalDateTime oldestPendingClaimAt) {
}
