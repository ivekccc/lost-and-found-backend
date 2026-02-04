package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyRequestDTO {

    @NotBlank(message = "Verification code is required")
    private String code;
}
