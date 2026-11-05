package com.example.demo.report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMinQuestionsRequestDto {

    @NotNull(message = "{validation.category.minQuestions.required}")
    @Min(value = 1, message = "{validation.category.minQuestions.min}")
    @Max(value = 10, message = "{validation.category.minQuestions.max}")
    private Integer minQuestions;
}
