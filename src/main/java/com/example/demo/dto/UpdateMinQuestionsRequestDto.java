package com.example.demo.dto;

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

    @NotNull(message = "Minimum questions is required")
    @Min(value = 1, message = "Minimum questions must be at least 1")
    @Max(value = 10, message = "Minimum questions must be at most 10")
    private Integer minQuestions;
}
