package com.example.demo.dto;

import com.example.demo.model.QuestionKind;
import com.example.demo.model.QuestionSource;
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

    @NotBlank(message = "Question prompt is required")
    @Size(max = 500, message = "Question prompt must be less than 500 characters")
    private String prompt;

    @NotNull(message = "Question kind is required")
    private QuestionKind kind;

    @NotNull(message = "Question source is required")
    private QuestionSource source;

    private Long templateId;

    @Size(max = 10, message = "Maximum 10 choices per question")
    private List<@NotBlank @Size(max = 100) String> choices;

    @Size(max = 500, message = "Correct answer must be less than 500 characters")
    private String correctAnswer;
}
