package com.example.demo.audit.controller;

import com.example.demo.audit.AuditAction;
import com.example.demo.audit.AuditTargetType;
import com.example.demo.audit.dto.AuditEntryDto;
import com.example.demo.audit.service.AuditService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole(\'ADMIN\')")
@RestController
@RequestMapping("/admin/audit-log")
@RequiredArgsConstructor
@Tag(name = "Admin audit log", description = "Who did what in moderation and configuration")
public class AdminAuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "List moderator actions",
            description = "Newest first, paginated, filterable by action, target (type and id) and actor. Entries are append-only.")
    @ApiResponse(responseCode = "200", description = "One page of audit entries")
    public ResponseEntity<Page<AuditEntryDto>> getEntries(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditTargetType targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return ResponseEntity.ok(auditService.getEntries(action, targetType, targetId, actorId, page, size, sort, direction));
    }
}
