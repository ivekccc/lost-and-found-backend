package com.example.demo.notification.service;

import com.example.demo.notification.NotificationFacade;
import com.example.demo.notification.model.NotificationType;
import com.example.demo.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultNotificationFacade implements NotificationFacade {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Override
    @Transactional
    public void notify(Long userId, NotificationType type, String title, String body, String dataJson) {
        notificationService.createNotification(userId, type, title, body, dataJson);
    }

    @Override
    public void sendVerificationEmail(String email, String code) {
        emailService.sendVerificationEmail(email, code);
    }

    @Override
    public void sendPasswordResetEmail(String email, String code) {
        emailService.sendPasswordResetEmail(email, code);
    }

    @Override
    @Transactional
    public void deleteNotificationsOfUser(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }
}
