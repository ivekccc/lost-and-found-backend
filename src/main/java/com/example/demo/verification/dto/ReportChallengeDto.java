package com.example.demo.verification.dto;

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
public class ReportChallengeDto {

    @NotNull
    private Long id;

    @NotBlank
    private String finderDisplayName;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private Integer questionCount;

    private Long myClaimId;

    private ClaimStatus myClaimStatus;

    @NotNull
    private Integer attemptsUsed;
}
