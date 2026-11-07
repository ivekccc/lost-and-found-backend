package com.example.demo.messaging;

import java.util.Optional;

public interface MessagingFacade {

    void openForClaim(Long claimId, Long reportId, Long holderId, Long claimantId);

    Optional<Long> findConversationIdForClaim(Long claimId);

    void deleteConversationsOfUser(Long userId);

    boolean hasThanksOnListing(Long reportId);

    long countThanksReceived(Long userId);

    Optional<ReportableMessage> findReportableMessage(Long messageId, Long reporterId);

    Optional<ReportableMessage> describeMessage(Long messageId);

    record ReportableMessage(Long messageId, Long conversationId, Long senderId, String excerpt) {
    }
}
