package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserRole", enumAsRef = true)
public enum UserRole {
    USER,
    ADMIN,
    COORDINATOR
}
