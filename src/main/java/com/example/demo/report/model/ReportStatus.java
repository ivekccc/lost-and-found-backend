package com.example.demo.report.model;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "ReportStatus", enumAsRef = true)
public enum ReportStatus {
    ACTIVE,
    MATCHED,
    RESOLVED,
    EXPIRED,
    FLAGGED,
    DELETED
}
