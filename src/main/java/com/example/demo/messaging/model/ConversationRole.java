package com.example.demo.messaging.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ConversationRole", enumAsRef = true)
public enum ConversationRole {
    HOLDER,
    CLAIMANT
}
