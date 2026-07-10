package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequestDto {

    @Schema(description = "Current password; required unless googleIdToken is provided")
    private String password;

    @Schema(description = "Fresh Google ID token; alternative confirmation for Google-linked accounts")
    private String googleIdToken;
}
