package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimAnswerDto {

    @NotNull
    private Long questionId;

    @NotBlank
    private String prompt;

    @NotBlank
    private String answerText;
}
