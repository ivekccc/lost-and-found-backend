package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ClaimStatus", enumAsRef = true)
public enum ClaimStatus {
    PENDING,
    APPROVED,
    DECLINED,
    WITHDRAWN
}
