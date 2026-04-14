package com.example.demo.controller;

import com.example.demo.dto.FcmTokenRequestDTO;
import com.example.demo.dto.NotificationDTO;
import com.example.demo.dto.UnreadCountDTO;
import com.example.demo.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Push notification endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get notifications (paginated)")
    public ResponseEntity<Page<NotificationDTO>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getNotifications(userDetails.getUsername(), pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<UnreadCountDTO> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        return
                ResponseEntity.ok(notificationService.getUnreadCount(userDetails.getUsername()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        notificationService.markAsRead(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {

        notificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/register-token")
    @Operation(summary = "Register FCM token")
    public ResponseEntity<Void> registerToken(@Valid @RequestBody FcmTokenRequestDTO fcmTokenRequestDTO, @AuthenticationPrincipal UserDetails userDetails) {
        notificationService.saveToken(userDetails.getUsername(), fcmTokenRequestDTO.getFcmToken());
        return ResponseEntity.ok().build();
    }


}
