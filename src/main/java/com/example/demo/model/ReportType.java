package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportType", enumAsRef = true)
public enum ReportType {
    LOST,
    FOUND
}
