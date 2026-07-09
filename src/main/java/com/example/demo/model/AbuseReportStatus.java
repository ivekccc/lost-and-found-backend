package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AbuseReportStatus", enumAsRef = true)
public enum AbuseReportStatus {
    PENDING,
    REVIEWED_ACTIONED,
    DISMISSED
}
