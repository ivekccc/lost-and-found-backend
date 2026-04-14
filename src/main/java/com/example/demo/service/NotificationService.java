package com.example.demo.service;

import com.example.demo.dto.NotificationDTO;
import com.example.demo.dto.UnreadCountDTO;
import com.example.demo.event.NotificationCreatedEvent;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Notification;
import com.example.demo.model.NotificationType;
import com.example.demo.model.PushStatus;
import com.example.demo.model.User;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void saveToken(String email, String fcmToken) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    @Transactional
    public Notification createNotification(Long userId, NotificationType type, String title, String body, String dataJson) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setDataJson(dataJson);

        if (user.getFcmToken() == null) {
            notification.setPushStatus(PushStatus.SKIPPED);
        }

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created [{}] for user {}", type, userId);

        eventPublisher.publishEvent(new NotificationCreatedEvent(this, saved.getId()));

        return saved;

    }


    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(),
                        pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public UnreadCountDTO getUnreadCount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        return new UnreadCountDTO(count);
    }


    @Transactional
    public void markAsRead(Long notificationId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int updated = notificationRepository.markAllAsReadByUserId(user.getId());
        log.info("Marked {} notifications as read for user {}", updated, user.getId());
    }

    private NotificationDTO toDTO(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getDataJson(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}
