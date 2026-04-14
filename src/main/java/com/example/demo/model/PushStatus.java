package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PushStatus")
public enum PushStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED
}