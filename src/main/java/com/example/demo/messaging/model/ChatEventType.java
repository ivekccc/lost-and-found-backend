package com.example.demo.messaging.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ChatEventType", enumAsRef = true)
public enum ChatEventType {
    MESSAGE,
    TYPING,
    READ
}
