package com.example.demo.controller;

import com.example.demo.dto.FcmTokenRequestDTO;
import com.example.demo.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Push notification endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/register-token")
    @Operation(summary = "Register FCM token")
    public ResponseEntity<Void> registerToken(@Valid @RequestBody FcmTokenRequestDTO fcmTokenRequestDTO, @AuthenticationPrincipal UserDetails userDetails) {
        notificationService.saveToken(userDetails.getUsername(),fcmTokenRequestDTO.getFcmToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/broadcast")
    @Operation(summary = "Send test notification to all users")
    public ResponseEntity<Void> broadcast(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.sendToAll("Lost and Found","Someone just posted a new report! Check it out");
        return ResponseEntity.ok().build();
    }
}
