package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.google.firebase.messaging.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserRepository userRepository;

    @Transactional
    public void saveToken(String email,String fcmToken){
        User user = userRepository.findByEmail(email).orElseThrow(()->new RuntimeException("User not found"));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }


    public void sendToAll(String title, String body) {
        List<User> users = userRepository.findByFcmTokenIsNotNull();
        if (users.isEmpty()) {
            log.info("No users with FCM tokens to notify");
            return;
        }

        List<Message> messages = users.stream()
                .map(user -> Message.builder()
                        .setToken(user.getFcmToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build())
                .toList();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);
            log.info("Broadcast sent: {} success, {} failures",
                    response.getSuccessCount(), response.getFailureCount());

            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse sendResponse = responses.get(i);
                if (!sendResponse.isSuccessful()) {
                    FirebaseMessagingException e = sendResponse.getException();
                    User user = users.get(i);
                    log.error("Failed to send to {}: {}", user.getEmail(), e.getMessagingErrorCode());

                    if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                            || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                        user.setFcmToken(null);
                        userRepository.save(user);
                        log.info("Removed stale token for user: {}", user.getEmail());
                    }
                }
            }
        } catch (FirebaseMessagingException e) {
            log.error("Broadcast failed: {}", e.getMessage());
        }
    }
}
