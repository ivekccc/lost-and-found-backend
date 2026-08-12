package com.example.demo.verification.dto;

import com.example.demo.verification.model.QuestionKind;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimReviewAnswerDto {

    @NotNull
    private Long questionId;

    @NotBlank
    private String prompt;

    @NotNull
    private QuestionKind kind;

    @NotBlank
    private String answerText;

    private Boolean isCorrect;

    private String expectedAnswer;
}
