package com.example.demo.user.model;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(name = "UserStatus", enumAsRef = true)
public enum UserStatus {
    ACTIVE,
    BLOCKED,
    PARTIALLY_BLOCKED,
    DELETED
}
