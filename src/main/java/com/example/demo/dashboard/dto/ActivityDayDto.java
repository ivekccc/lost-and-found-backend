package com.example.demo.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(name = "ActivityDayDto", description = "Platform activity on one calendar day.")
public record ActivityDayDto(
        @NotNull LocalDate day,
        @NotNull long reportsPosted,
        @NotNull long reportsReunited,
        @NotNull long usersRegistered) {
}
