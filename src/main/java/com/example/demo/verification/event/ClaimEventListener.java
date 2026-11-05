package com.example.demo.verification.event;

import com.example.demo.notification.model.NotificationType;
import com.example.demo.notification.NotificationFacade;
import com.example.demo.verification.model.Claim;

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

    private final NotificationFacade notificationFacade;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onClaimSubmitted(ClaimSubmittedEvent event) {
        log.info("ClaimSubmittedEvent received for claim {}", event.getClaimId());
        notificationFacade.notify(
                event.getChallengeAuthorId(),
                NotificationType.CLAIM_SUBMITTED,
                "notification.claimSubmitted.title",
                "notification.claimSubmitted.body",
                "{\"reportId\":" + event.getReportId() + ",\"challengeId\":" + event.getChallengeId() + ",\"claimId\":" + event.getClaimId() + "}",
                event.getReportTitle());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onClaimDecided(ClaimDecidedEvent event) {
        log.info("ClaimDecidedEvent received for claim {} (approved={})", event.getClaimId(), event.isApproved());
        String dataJson = "{\"reportId\":" + event.getReportId() + ",\"challengeId\":" + event.getChallengeId() + ",\"claimId\":" + event.getClaimId() + "}";

        if (event.isApproved()) {
            notificationFacade.notify(
                    event.getClaimantId(),
                    NotificationType.CLAIM_APPROVED,
                    "notification.claimApproved.title",
                    "notification.claimApproved.body",
                    dataJson,
                    event.getReportTitle());
        } else {
            notificationFacade.notify(
                    event.getClaimantId(),
                    NotificationType.CLAIM_DECLINED,
                    "notification.claimDeclined.title",
                    "notification.claimDeclined.body",
                    dataJson,
                    event.getReportTitle());
        }
    }
}
