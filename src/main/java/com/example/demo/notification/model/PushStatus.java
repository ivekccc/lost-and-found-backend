package com.example.demo.notification.model;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "PushStatus")
public enum PushStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED
}
