package com.example.demo.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimAnswerRequestDto {

    @NotNull(message = "{validation.answer.questionId.required}")
    private Long questionId;

    @NotBlank(message = "{validation.answer.text.required}")
    @Size(max = 1000, message = "{validation.answer.text.size}")
    private String answerText;
}
