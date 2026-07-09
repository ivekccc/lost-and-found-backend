package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AbuseReason", enumAsRef = true)
public enum AbuseReason {
    SCAM,
    SPAM,
    OFFENSIVE,
    PERSONAL_INFO,
    WRONG_CATEGORY,
    OTHER
}
