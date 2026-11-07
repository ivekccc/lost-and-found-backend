package com.example.demo.audit;

import java.util.Map;

public record AdminActionEvent(
        AuditAction action,
        AuditTargetType targetType,
        Long targetId,
        String summary,
        Map<String, Object> details) {

    public static AdminActionEvent of(AuditAction action, AuditTargetType targetType, Long targetId, String summary) {
        return new AdminActionEvent(action, targetType, targetId, summary, null);
    }
}
