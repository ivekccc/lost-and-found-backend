package com.example.demo.verification.admin;

import com.example.demo.report.model.ReportType;
import com.example.demo.verification.model.ClaimStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminClaimListDto {

    @NotNull
    private Long id;

    @NotNull
    private Long reportId;

    @NotBlank
    private String reportTitle;

    @NotNull
    private ReportType reportType;

    @NotNull
    private Long challengeId;

    private String challengeAuthorName;

    private String claimantName;

    @NotNull
    private ClaimStatus status;

    @NotNull
    private LocalDateTime submittedAt;

    private LocalDateTime decidedAt;
}
