package com.example.demo.dto;

import com.example.demo.model.QuestionKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminQuestionTemplateDto {

    @NotNull
    private Long id;

    @NotNull
    private Long categoryId;

    @NotBlank
    private String categoryName;

    @NotBlank
    private String prompt;

    @NotNull
    private QuestionKind kind;

    private List<String> defaultChoices;

    @NotNull
    private Boolean isActive;

    @NotNull
    private LocalDateTime createdAt;
}
