package com.example.demo.messaging.dto;

import com.example.demo.messaging.model.ChatEventType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChatEventDto",
        description = "Frame pushed over the /ws STOMP connection to /user/queue/chat. MESSAGE carries the "
                + "message, TYPING the user who is typing, READ the user and the moment they read up to.")
public class ChatEventDto {

    @NotNull
    private ChatEventType type;

    @NotNull
    private Long conversationId;

    private MessageDto message;

    private Long userId;

    private LocalDateTime readAt;
}
