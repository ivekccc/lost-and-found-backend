package com.example.demo.dto;

import com.example.demo.model.ClaimStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDto {

    @NotNull
    private Long id;

    @NotNull
    private Long challengeId;

    @NotNull
    private Long reportId;

    @NotBlank
    private String reportTitle;

    @NotNull
    private ClaimStatus status;

    private String message;

    private String photoUrl;

    @NotNull
    private LocalDateTime submittedAt;

    private LocalDateTime decidedAt;

    @NotNull
    private List<ClaimAnswerDto> answers;

    private RevealedContactDto holderContact;
}
