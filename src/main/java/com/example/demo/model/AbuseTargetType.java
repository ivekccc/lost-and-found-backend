package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AbuseTargetType", enumAsRef = true)
public enum AbuseTargetType {
    USER,
    REPORT
}
