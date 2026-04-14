package com.example.demo.service;

import com.example.demo.model.Notification;
import com.example.demo.model.PushStatus;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.PreRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleTasks {

    private final PreRegistrationRepository preRegistrationRepository;
    private final NotificationRepository notificationRepository;
    private final FcmPushSender fcmPushSender;

    @Value("${app.push.max-retry-count:3}")
    private int maxRetryCount;

    @Scheduled(fixedRateString = "${app.cleanup.interval-ms:3600000}")
    @Transactional
    public void cleanupExpiredPreRegistrations() {
        int deleted = preRegistrationRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        if (deleted > 0) {
            log.info("Cleaned up {} expired pre-registrations", deleted);
        }
    }


    @Scheduled(fixedRate = 300000)
    @Transactional
    public void retryFailedPushNotifications() {
        List<Notification> failed = notificationRepository.findByPushStatusAndNextRetryAtBefore(PushStatus.FAILED, LocalDateTime.now());

        if (failed.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed push notifications", failed.size());

        for (Notification notification : failed) {
            String fcmToken = notification.getUser().getFcmToken();

            if (fcmToken == null) {
                notification.setPushStatus(PushStatus.SKIPPED);
                notificationRepository.save(notification);
                continue;
            }

            Map<String, String> data = null;
            if (notification.getDataJson() != null) {
                data = Map.of(
                        "type", notification.getType().name(),
                        "payload", notification.getDataJson()
                );
            }

            FcmPushSender.SendResult result = fcmPushSender.sendToToken(fcmToken, notification.getTitle(), notification.getBody(), data);

            if (result.success()) {
                notification.setPushStatus(PushStatus.SENT);
                notification.setSentPushAt(LocalDateTime.now());
            } else if (result.tokenInvalid()) {
                notification.setPushStatus(PushStatus.SKIPPED);
                fcmPushSender.removeStaleToken(notification.getUser().getId());
            } else {
                int newRetryCount = notification.getRetryCount() + 1;
                notification.setRetryCount(newRetryCount);

                if (newRetryCount >= maxRetryCount) {
                    notification.setPushStatus(PushStatus.SKIPPED);
                    log.warn("Giving up on notification {} after {} retries",
                            notification.getId(), newRetryCount);
                } else {
                    long delayMinutes = (long) Math.pow(2, newRetryCount) * 2;

                    notification.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
                }
            }
            notificationRepository.save(notification);
        }
    }


}
