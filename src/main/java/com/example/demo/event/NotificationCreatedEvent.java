package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationCreatedEvent extends ApplicationEvent {

    private final Long notificationId;

    public NotificationCreatedEvent(Object source, Long notificationId) {
        super(source);
        this.notificationId = notificationId;
    }
}
