package com.example.demo.service;

import com.example.demo.repository.UserRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushSender {

    private final UserRepository userRepository;

    public SendResult sendToToken(String fcmToken, String title, String body, Map<String, String> data) {
        Message.Builder builder = Message.builder().setToken(fcmToken).setNotification(
                Notification.builder().setTitle(title).setBody(body).build()
        );

        if (data != null) {
            builder.putAllData(data);
        }

        try {
            String messageId = FirebaseMessaging.getInstance().send(builder.build());
            log.debug("Push sent successfully: {}", messageId);
            return new SendResult(true, null, false);
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode errorCode = e.getMessagingErrorCode();
            log.error("Push failed [{}]: {}", errorCode, e.getMessage());

            boolean tokenInvalid = errorCode == MessagingErrorCode.UNREGISTERED
                    || errorCode == MessagingErrorCode.INVALID_ARGUMENT
                    || errorCode == MessagingErrorCode.SENDER_ID_MISMATCH;

            return new SendResult(false, e.getMessage(), tokenInvalid);
        }
    }

    public void removeStaleToken(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFcmToken(null);
            userRepository.save(user);
            log.info("Removed stale FCM token for user {}", userId);
        });
    }

    public record SendResult(boolean success, String error, boolean tokenInvalid) {
    }
}
