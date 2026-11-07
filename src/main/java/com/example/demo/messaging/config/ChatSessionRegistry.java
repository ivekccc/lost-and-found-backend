package com.example.demo.messaging.config;

import com.example.demo.user.event.UserSessionsEndedEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
@Slf4j
@Component
public class ChatSessionRegistry {

    private final Map<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionIdsByUser = new ConcurrentHashMap<>();

    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessionsById.put(session.getId(), session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                sessionsById.remove(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (user != null && sessionId != null) {
            sessionIdsByUser.computeIfAbsent(user.getName(), ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
        }
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user == null) {
            return;
        }
        sessionIdsByUser.computeIfPresent(user.getName(), (name, ids) -> {
            ids.remove(event.getSessionId());
            return ids.isEmpty() ? null : ids;
        });
    }

    @TransactionalEventListener
    public void onSessionsEnded(UserSessionsEndedEvent event) {
        Set<String> sessionIds = sessionIdsByUser.remove(event.email());
        if (sessionIds == null) {
            return;
        }
        for (String sessionId : sessionIds) {
            WebSocketSession session = sessionsById.remove(sessionId);
            if (session == null) {
                continue;
            }
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException e) {
                log.warn("Could not close chat session {}", sessionId, e);
            }
        }
    }
}
