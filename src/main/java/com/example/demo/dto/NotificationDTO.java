package com.example.demo.dto;

import com.example.demo.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    @NotNull
    private Long id;

    @NotNull
    private NotificationType type;

    @NotBlank
    private String title;

    @NotBlank
    private String body;

    private String dataJson;

    @NotNull
    private Boolean isRead;

    @NotNull
    private LocalDateTime createdAt;
}
