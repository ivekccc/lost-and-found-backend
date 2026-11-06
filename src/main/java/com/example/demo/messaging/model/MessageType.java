package com.example.demo.messaging.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MessageType", enumAsRef = true)
public enum MessageType {
    TEXT,
    MEETING_POINT,
    CONTACT
}
