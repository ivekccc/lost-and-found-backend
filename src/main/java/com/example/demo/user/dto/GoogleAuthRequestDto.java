package com.example.demo.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class GoogleAuthRequestDto {

    @NotBlank(message = "idToken is required")
    private String idToken;
}
