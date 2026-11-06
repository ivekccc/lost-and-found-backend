package com.example.demo.messaging.admin;

import com.example.demo.messaging.model.Conversation;
import com.example.demo.messaging.service.MessagingService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminConversationService {

    private final MessagingService messagingService;

    @Transactional(readOnly = true)
    public AdminConversationDto getConversation(Long conversationId) {
        Conversation conversation = messagingService.requireForModeration(conversationId);
        return new AdminConversationDto(
                conversation.getId(),
                conversation.getReportId(),
                messagingService.reportTitleOf(conversation),
                conversation.getHolder().getId(),
                messagingService.displayName(conversation.getHolder()),
                conversation.getClaimant().getId(),
                messagingService.displayName(conversation.getClaimant()),
                messagingService.getAllMessagesForModeration(conversation.getId()));
    }
}
