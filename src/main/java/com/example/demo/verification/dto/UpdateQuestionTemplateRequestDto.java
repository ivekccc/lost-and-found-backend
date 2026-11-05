package com.example.demo.verification.dto;

import com.example.demo.verification.model.QuestionKind;

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
public class UpdateQuestionTemplateRequestDto {

    @NotBlank(message = "{validation.template.prompt.required}")
    @Size(max = 500, message = "{validation.template.prompt.size}")
    private String prompt;

    @NotBlank(message = "{validation.template.promptSr.required}")
    @Size(max = 500, message = "{validation.template.prompt.size}")
    private String promptSr;

    @NotNull(message = "{validation.template.kind.required}")
    private QuestionKind kind;

    @Size(max = 10, message = "{validation.template.defaultChoices.size}")
    private List<@NotBlank @Size(max = 100) String> defaultChoices;

    @Size(max = 10, message = "{validation.template.defaultChoices.size}")
    private List<@NotBlank @Size(max = 100) String> defaultChoicesSr;

    @NotNull(message = "{validation.template.active.required}")
    private Boolean isActive;
}
