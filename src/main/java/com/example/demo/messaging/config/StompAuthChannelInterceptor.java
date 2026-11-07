package com.example.demo.messaging.config;

import com.example.demo.shared.security.AuthenticatedUser;
import com.example.demo.shared.security.JwtUtil;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String OWN_QUEUE_PREFIX = "/user/queue/";
    private static final String APPLICATION_PREFIX = "/app/";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        } else if (command == StompCommand.SUBSCRIBE) {
            requireUser(accessor);
            String destination = accessor.getDestination();
            if (destination == null || !destination.startsWith(OWN_QUEUE_PREFIX)) {
                throw new MessagingException("Subscription not allowed");
            }
        } else if (command == StompCommand.SEND) {
            requireUser(accessor);
            String destination = accessor.getDestination();
            if (destination == null || !destination.startsWith(APPLICATION_PREFIX)) {
                throw new MessagingException("Destination not allowed");
            }
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new MessagingException("Missing token");
        }
        String token = header.substring(BEARER_PREFIX.length());
        try {
            if (!(userDetailsService.loadUserByUsername(jwtUtil.extractUsername(token)) instanceof AuthenticatedUser user)
                    || !user.isEnabled() || !jwtUtil.validateToken(token, user)) {
                throw new MessagingException("Invalid token");
            }
            return new UsernamePasswordAuthenticationToken(user.getUsername(), null, user.getAuthorities());
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
            throw new MessagingException("Invalid token");
        }
    }

    private void requireUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new MessagingException("Not authenticated");
        }
    }
}
