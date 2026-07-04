package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChallengeRequestDto {

    @Valid
    @NotEmpty(message = "Verification questions are required")
    @Size(max = 10, message = "Maximum 10 verification questions per challenge")
    private List<ChallengeQuestionRequestDto> questions;
}
