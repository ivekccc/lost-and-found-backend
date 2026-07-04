package com.example.demo.event;

import com.example.demo.model.NotificationType;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onClaimSubmitted(ClaimSubmittedEvent event) {
        log.info("ClaimSubmittedEvent received for claim {}", event.getClaimId());
        notificationService.createNotification(
                event.getChallengeAuthorId(),
                NotificationType.CLAIM_SUBMITTED,
                "New ownership claim",
                "Someone answered your verification questions for \"" + event.getReportTitle() + "\". Review their claim.",
                "{\"reportId\":" + event.getReportId() + ",\"challengeId\":" + event.getChallengeId() + ",\"claimId\":" + event.getClaimId() + "}"
        );
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onClaimDecided(ClaimDecidedEvent event) {
        log.info("ClaimDecidedEvent received for claim {} (approved={})", event.getClaimId(), event.isApproved());
        String dataJson = "{\"reportId\":" + event.getReportId() + ",\"challengeId\":" + event.getChallengeId() + ",\"claimId\":" + event.getClaimId() + "}";

        if (event.isApproved()) {
            notificationService.createNotification(
                    event.getClaimantId(),
                    NotificationType.CLAIM_APPROVED,
                    "Ownership confirmed",
                    "Your claim for \"" + event.getReportTitle() + "\" was confirmed. Contact details are now available.",
                    dataJson
            );
        } else {
            notificationService.createNotification(
                    event.getClaimantId(),
                    NotificationType.CLAIM_DECLINED,
                    "Claim declined",
                    "Your claim for \"" + event.getReportTitle() + "\" was declined.",
                    dataJson
            );
        }
    }
}
