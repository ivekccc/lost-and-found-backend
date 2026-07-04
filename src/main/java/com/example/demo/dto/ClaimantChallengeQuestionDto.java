package com.example.demo.dto;

import com.example.demo.model.QuestionKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimantChallengeQuestionDto {

    @NotNull
    private Long id;

    @NotBlank
    private String prompt;

    @NotNull
    private QuestionKind kind;

    private List<String> choices;

    @NotNull
    private Integer orderIndex;
}
