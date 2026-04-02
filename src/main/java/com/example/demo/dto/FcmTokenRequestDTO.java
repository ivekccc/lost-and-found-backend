package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FcmTokenRequestDTO {
    @NotBlank(message = "FCM token is required")
    private String fcmToken;
}
