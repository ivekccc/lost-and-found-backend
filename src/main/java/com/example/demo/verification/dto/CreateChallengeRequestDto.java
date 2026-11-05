package com.example.demo.verification.dto;

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
    @NotEmpty(message = "{validation.challenge.questions.required}")
    @Size(max = 10, message = "{validation.challenge.questions.size}")
    private List<ChallengeQuestionRequestDto> questions;
}
