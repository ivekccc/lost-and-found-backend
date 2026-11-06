package com.example.demo.messaging.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatPresenceRegistry {

    private final Map<String, Integer> sessionsByUser = new ConcurrentHashMap<>();

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            sessionsByUser.merge(user.getName(), 1, Integer::sum);
        }
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            sessionsByUser.computeIfPresent(user.getName(), (name, count) -> count > 1 ? count - 1 : null);
        }
    }

    public boolean isOnline(String email) {
        return sessionsByUser.containsKey(email);
    }
}
