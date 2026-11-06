package com.example.demo.verification.dto;

import com.example.demo.verification.model.ClaimStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDetailsDto {

    @NotNull
    private Long id;

    @NotNull
    private Long challengeId;

    @NotNull
    private Long reportId;

    @NotNull
    private ClaimStatus status;

    @NotNull
    private Long claimantId;

    private String claimantName;

    private String message;

    private String photoUrl;

    @NotNull
    private LocalDateTime submittedAt;

    private LocalDateTime decidedAt;

    @NotNull
    private List<ClaimReviewAnswerDto> answers;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Conversation with the claimant, once the claim is approved.")
    private Long conversationId;
}
