package com.example.demo.messaging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ConversationUnreadCountDto", description = "Conversations with at least one unread message.")
public class ConversationUnreadCountDto {

    @NotNull
    private Long count;
}
