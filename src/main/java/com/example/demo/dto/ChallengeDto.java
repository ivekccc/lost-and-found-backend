package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDto {

    @NotNull
    private Long id;

    @NotNull
    private Long reportId;

    @NotNull
    private Long authorId;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private List<ChallengeQuestionDto> questions;
}
