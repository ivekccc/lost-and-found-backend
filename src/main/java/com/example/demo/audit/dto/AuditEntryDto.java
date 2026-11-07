package com.example.demo.audit.dto;

import com.example.demo.audit.AuditAction;
import com.example.demo.audit.AuditTargetType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(name = "AuditEntryDto", description = "One moderator action. Entries are never edited or deleted.")
public record AuditEntryDto(
        @NotNull Long id,
        @Schema(description = "Moderator who acted; null once that account is deleted, the name stays.")
        Long actorId,
        @NotBlank String actorName,
        @NotNull AuditAction action,
        @NotNull AuditTargetType targetType,
        Long targetId,
        @NotBlank String summary,
        @Schema(description = "Action-specific values, such as the previous and new matching weights.")
        Map<String, Object> details,
        @NotNull LocalDateTime createdAt) {
}
