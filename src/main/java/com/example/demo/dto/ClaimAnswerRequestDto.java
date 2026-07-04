package com.example.demo.dto;

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

    @NotNull(message = "Question id is required")
    private Long questionId;

    @NotBlank(message = "Answer is required")
    @Size(max = 1000, message = "Answer must be less than 1000 characters")
    private String answerText;
}
