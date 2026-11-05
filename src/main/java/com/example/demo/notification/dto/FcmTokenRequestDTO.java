package com.example.demo.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class FcmTokenRequestDTO {
    @NotBlank(message = "{validation.fcmToken.required}")
    private String fcmToken;
}
