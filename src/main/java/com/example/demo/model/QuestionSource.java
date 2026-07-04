package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "QuestionSource", enumAsRef = true)
public enum QuestionSource {
    TEMPLATE,
    CUSTOM
}
