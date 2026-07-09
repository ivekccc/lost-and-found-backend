package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class UserDeletedEvent extends ApplicationEvent {

    private final Long userId;
    private final List<String> imagePublicIds;

    public UserDeletedEvent(Object source, Long userId, List<String> imagePublicIds) {
        super(source);
        this.userId = userId;
        this.imagePublicIds = imagePublicIds;
    }
}
