package com.example.demo.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class RefreshTokenRequestDTO {

    @NotBlank(message = "{validation.refreshToken.required}")
    private String refreshToken;
}
