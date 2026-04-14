package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NotificationType", enumAsRef = true)
public enum NotificationType {
    REPORT_CREATED,
    MATCH_FOUND,
    REPORT_EXPIRED,
    REPORT_RESOLVED
}