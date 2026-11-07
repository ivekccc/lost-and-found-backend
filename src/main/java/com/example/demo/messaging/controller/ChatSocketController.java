package com.example.demo.messaging.controller;

import com.example.demo.messaging.dto.ChatEventDto;
import com.example.demo.messaging.event.ChatEventListener;
import com.example.demo.messaging.model.ChatEventType;
import com.example.demo.messaging.service.MessagingService;
import com.example.demo.shared.ratelimit.RateLimitPolicy;
import com.example.demo.shared.ratelimit.RateLimiter;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final MessagingService messagingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RateLimiter rateLimiter;

    @MessageMapping("/conversations/{conversationId}/typing")
    public void typing(@DestinationVariable Long conversationId, Principal principal) {
        if (principal == null || !rateLimiter.tryConsume(RateLimitPolicy.TYPING_PER_USER, principal.getName())) {
            return;
        }
        messagingService.typingTarget(conversationId, principal.getName())
                .ifPresent(target -> messagingTemplate.convertAndSendToUser(
                        target.otherEmail(), ChatEventListener.USER_QUEUE,
                        new ChatEventDto(ChatEventType.TYPING, conversationId, null, target.typistId(), null)));
    }
}
