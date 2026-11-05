package com.example.demo.user.dto;

import com.example.demo.user.model.AppLanguage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "UpdateLanguageRequestDto", description = "Language the user picked in the app")
public class UpdateLanguageRequestDto {

    @NotNull(message = "{validation.language.required}")
    private AppLanguage language;
}
