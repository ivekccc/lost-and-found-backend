package com.example.demo.messaging.event;

import com.example.demo.messaging.service.MessagingService;
import com.example.demo.verification.event.ClaimDecidedEvent;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ClaimApprovedListener {

    private final MessagingService messagingService;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onClaimDecided(ClaimDecidedEvent event) {
        if (event.isApproved()) {
            messagingService.openForClaim(
                    event.getClaimId(), event.getReportId(), event.getHolderId(), event.getClaimantId());
        }
    }
}
