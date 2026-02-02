package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportStatus", enumAsRef = true)
public enum ReportStatus {
    ACTIVE,
    RESOLVED,
    EXPIRED,
    FLAGGED,
    DELETED
}
