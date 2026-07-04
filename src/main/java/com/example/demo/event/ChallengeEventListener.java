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
public class ChallengeEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onChallengeCreated(ChallengeCreatedEvent event) {
        log.info("ChallengeCreatedEvent received for challenge {}", event.getChallengeId());
        notificationService.createNotification(
                event.getReportOwnerId(),
                NotificationType.CHALLENGE_CREATED,
                "Someone thinks they found your item",
                "A finder responded to \"" + event.getReportTitle() + "\". Answer their verification questions to prove ownership.",
                "{\"reportId\":" + event.getReportId() + ",\"challengeId\":" + event.getChallengeId() + "}"
        );
    }
}
