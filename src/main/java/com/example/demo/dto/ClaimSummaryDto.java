package com.example.demo.dto;

import com.example.demo.model.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSummaryDto {

    @NotNull
    private Long id;

    @NotNull
    private ClaimStatus status;

    private String claimantName;

    @NotNull
    private LocalDateTime submittedAt;

    @NotNull
    private Integer correctChoiceAnswers;

    @NotNull
    private Integer totalChoiceAnswers;

    @NotNull
    private Boolean hasPhoto;
}
