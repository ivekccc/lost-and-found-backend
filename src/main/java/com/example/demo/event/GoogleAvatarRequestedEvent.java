package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GoogleAvatarRequestedEvent extends ApplicationEvent {

    private final Long userId;
    private final String pictureUrl;

    public GoogleAvatarRequestedEvent(Object source, Long userId, String pictureUrl) {
        super(source);
        this.userId = userId;
        this.pictureUrl = pictureUrl;
    }
}
