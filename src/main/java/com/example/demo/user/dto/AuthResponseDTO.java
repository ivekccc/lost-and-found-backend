package com.example.demo.user.dto;

import com.example.demo.user.model.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    @NotBlank
    private String accessToken;

    @NotBlank
    private String refreshToken;

    @NotNull
    private UserRole role;
}
