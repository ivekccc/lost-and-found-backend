package com.example.demo.verification.dto;

import com.example.demo.verification.model.QuestionKind;
import com.example.demo.verification.model.QuestionSource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeQuestionDto {

    @NotNull
    private Long id;

    @NotBlank
    private String prompt;

    @NotNull
    private QuestionKind kind;

    @NotNull
    private QuestionSource source;

    private Long templateId;

    private List<String> choices;

    private String correctAnswer;

    @NotNull
    private Integer orderIndex;
}
