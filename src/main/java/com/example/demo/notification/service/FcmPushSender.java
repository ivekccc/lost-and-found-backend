package com.example.demo.notification.service;

import com.example.demo.notification.model.NotificationType;
import com.example.demo.user.UserFacade;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushSender {

    static final String MESSAGES_CHANNEL = "messages";
    static final String GENERAL_CHANNEL = "general";
    private static final String TYPE_KEY = "type";

    private final UserFacade userFacade;

    public SendResult sendToToken(String fcmToken, String title, String body, Map<String, String> data) {
        boolean chat = data != null && NotificationType.CHAT_MESSAGE.name().equals(data.get(TYPE_KEY));
        Message.Builder builder = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(AndroidNotification.builder()
                                .setChannelId(chat ? MESSAGES_CHANNEL : GENERAL_CHANNEL)
                                .setVisibility(chat ? AndroidNotification.Visibility.PRIVATE : AndroidNotification.Visibility.PUBLIC)
                                .build())
                        .build());

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
        userFacade.clearPushToken(userId);
        log.info("Removed stale FCM token for user {}", userId);
    }

    public record SendResult(boolean success, String error, boolean tokenInvalid) {
    }
}
