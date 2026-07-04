package com.example.demo.dto;

import com.example.demo.model.QuestionKind;
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
public class CreateQuestionTemplateRequestDto {

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotBlank(message = "Prompt is required")
    @Size(max = 500, message = "Prompt must be less than 500 characters")
    private String prompt;

    @NotNull(message = "Kind is required")
    private QuestionKind kind;

    @Size(max = 10, message = "Maximum 10 default choices")
    private List<@NotBlank @Size(max = 100) String> defaultChoices;
}
