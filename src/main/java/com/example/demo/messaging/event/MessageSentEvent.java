package com.example.demo.messaging.event;

import com.example.demo.messaging.dto.MessageDto;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MessageSentEvent extends ApplicationEvent {

    private final MessageDto message;
    private final String senderEmail;
    private final String senderName;
    private final Long recipientId;
    private final String recipientEmail;

    public MessageSentEvent(Object source, MessageDto message, String senderEmail, String senderName,
                            Long recipientId, String recipientEmail) {
        super(source);
        this.message = message;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.recipientEmail = recipientEmail;
    }
}
