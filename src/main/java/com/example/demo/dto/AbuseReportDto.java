package com.example.demo.dto;

import com.example.demo.model.AbuseReason;
import com.example.demo.model.AbuseReportStatus;
import com.example.demo.model.AbuseTargetType;
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
