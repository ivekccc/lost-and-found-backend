package com.example.demo.messaging.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class ConversationReadEvent extends ApplicationEvent {

    private final Long conversationId;
    private final Long readerId;
    private final LocalDateTime readAt;
    private final String otherEmail;

    public ConversationReadEvent(Object source, Long conversationId, Long readerId, LocalDateTime readAt,
                                 String otherEmail) {
        super(source);
        this.conversationId = conversationId;
        this.readerId = readerId;
        this.readAt = readAt;
        this.otherEmail = otherEmail;
    }
}
