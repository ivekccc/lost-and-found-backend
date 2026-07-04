package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "QuestionKind", enumAsRef = true)
public enum QuestionKind {
    TEXT,
    CHOICE
}
