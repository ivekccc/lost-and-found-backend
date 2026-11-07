package com.example.demo.audit;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuditTargetType", enumAsRef = true)
public enum AuditTargetType {
    REPORT,
    USER,
    ABUSE_REPORT,
    CATEGORY,
    QUESTION_TEMPLATE,
    MATCHING_CONFIG
}
