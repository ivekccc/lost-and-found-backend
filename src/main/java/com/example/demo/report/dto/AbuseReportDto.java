package com.example.demo.report.dto;

import com.example.demo.report.model.AbuseReason;
import com.example.demo.report.model.AbuseReportStatus;
import com.example.demo.report.model.AbuseTargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbuseReportDto {

    @NotNull
    private Long id;

    @NotBlank
    private String reporterName;

    @NotNull
    private AbuseTargetType targetType;

    private Long targetUserId;

    private Long targetReportId;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Reported chat message, when the target is a message.")
    private Long targetMessageId;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Conversation of the reported message, for reading it in context.")
    private Long targetConversationId;

    @NotBlank
    private String targetLabel;

    @NotNull
    private AbuseReason reason;

    private String message;

    @NotNull
    private AbuseReportStatus status;

    @NotNull
    private LocalDateTime createdAt;

    private String reviewedByName;

    private LocalDateTime reviewedAt;

    private String resolutionNote;
}
