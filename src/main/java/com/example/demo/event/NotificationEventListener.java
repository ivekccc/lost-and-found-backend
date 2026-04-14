package com.example.demo.event;

import com.example.demo.model.Notification;
import com.example.demo.model.PushStatus;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.service.FcmPushSender;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final FcmPushSender fcmPushSender;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNotificationCreated(NotificationCreatedEvent notificationCreatedEvent) {
        Notification notification = notificationRepository.findById(notificationCreatedEvent.getNotificationId()).orElse(null);

        if (notification == null || notification.getPushStatus() == PushStatus.SKIPPED) {
            return;
        }

        String fcmToken = notification.getUser().getFcmToken();
        if (fcmToken == null || fcmToken.isEmpty()) {
            notification.setPushStatus(PushStatus.SKIPPED);
            notificationRepository.save(notification);
            return;
        }

        Map<String, String> data = null;

        if (notification.getDataJson() != null) {
            data = Map.of(
                    "type", notification.getType().name(),
                    "payload", notification.getDataJson()
            );
        }

        FcmPushSender.SendResult result = fcmPushSender.sendToToken(
                fcmToken,
                notification.getTitle(),
                notification.getBody(),
                data);

        if (result.success()) {
            notification.setPushStatus(PushStatus.SENT);
            notification.setSentPushAt(LocalDateTime.now());
        } else {
            notification.setPushError(result.error());
            if (result.tokenInvalid()) {
                notification.setPushStatus(PushStatus.SKIPPED);
                fcmPushSender.removeStaleToken(notification.getUser().getId());
            } else {
                notification.setPushStatus(PushStatus.FAILED);
                notification.setRetryCount(1);
                notification.setNextRetryAt(LocalDateTime.now().plusMinutes(2));
            }
        }

        notificationRepository.save(notification);
    }

}
