package com.example.demo.messaging.service;

import com.example.demo.messaging.MessagingFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultMessagingFacade implements MessagingFacade {

    private final MessagingService messagingService;

    @Override
    public void openForClaim(Long claimId, Long reportId, Long holderId, Long claimantId) {
        messagingService.openForClaim(claimId, reportId, holderId, claimantId);
    }

    @Override
    public Optional<Long> findConversationIdForClaim(Long claimId) {
        return messagingService.findConversationIdForClaim(claimId);
    }

    @Override
    public void deleteConversationsOfUser(Long userId) {
        messagingService.deleteConversationsOfUser(userId);
    }

    @Override
    public Optional<ReportableMessage> findReportableMessage(Long messageId, Long reporterId) {
        return messagingService.findReportableMessage(messageId, reporterId);
    }

    @Override
    public Optional<ReportableMessage> describeMessage(Long messageId) {
        return messagingService.describeMessage(messageId);
    }
}
