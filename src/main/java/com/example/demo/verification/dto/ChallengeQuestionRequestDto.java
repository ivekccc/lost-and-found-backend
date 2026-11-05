package com.example.demo.verification.dto;

import com.example.demo.verification.model.QuestionKind;
import com.example.demo.verification.model.QuestionSource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeQuestionRequestDto {

    @NotBlank(message = "{validation.question.prompt.required}")
    @Size(max = 500, message = "{validation.question.prompt.size}")
    private String prompt;

    @NotNull(message = "{validation.question.kind.required}")
    private QuestionKind kind;

    @NotNull(message = "{validation.question.source.required}")
    private QuestionSource source;

    private Long templateId;

    @Size(max = 10, message = "{validation.question.choices.size}")
    private List<@NotBlank @Size(max = 100) String> choices;

    @Size(max = 500, message = "{validation.question.correctAnswer.size}")
    private String correctAnswer;
}
