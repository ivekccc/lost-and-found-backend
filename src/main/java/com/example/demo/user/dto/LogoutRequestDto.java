package com.example.demo.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequestDto {

    @NotBlank(message = "{validation.refreshToken.required}")
    private String refreshToken;

    @Schema(description = "Push token of this device. Sent so the server stops pushing to a device "
            + "that has signed out; cleared only when it matches the one on record, since another "
            + "device may still be signed in to the same account.")
    private String fcmToken;
}
