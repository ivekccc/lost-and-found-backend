package com.example.demo.notification;

import com.example.demo.notification.model.NotificationType;

/**
 * The only entry point other modules may use to raise a notification or send mail.
 *
 * Modules pass the type and the copy; whether that ends up in the inbox, as a push, or both is
 * this module's business.
 */
public interface NotificationFacade {

    void notify(Long userId, NotificationType type, String title, String body, String dataJson);

    void sendVerificationEmail(String email, String code);

    void sendPasswordResetEmail(String email, String code);

    void deleteNotificationsOfUser(Long userId);
}
