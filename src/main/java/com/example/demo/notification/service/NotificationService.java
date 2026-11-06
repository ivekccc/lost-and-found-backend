package com.example.demo.notification.service;

import java.util.Locale;

import org.springframework.context.MessageSource;

import com.example.demo.notification.dto.NotificationDTO;
import com.example.demo.notification.dto.UnreadCountDTO;
import com.example.demo.notification.event.NotificationCreatedEvent;
import com.example.demo.notification.model.Notification;
import com.example.demo.notification.model.NotificationType;
import com.example.demo.notification.model.PushStatus;
import com.example.demo.notification.repository.NotificationRepository;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.model.User;
import com.example.demo.user.UserFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Map<NotificationType, Integer> DAILY_PUSH_LIMITS =
            Map.of(NotificationType.SAVED_SEARCH_MATCH, 5);

    private final NotificationRepository notificationRepository;
    private final UserFacade userFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageSource messageSource;

    public void saveToken(String email, String fcmToken) {
        userFacade.savePushToken(email, fcmToken);
    }

    @Transactional
    public Notification createNotification(Long userId, NotificationType type, String titleKey, String bodyKey,
                                           String dataJson, Object... arguments) {
        User user = userFacade.requireById(userId);
        Locale locale = user.getLanguage().toLocale();

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(messageSource.getMessage(titleKey, arguments, locale));
        notification.setBody(messageSource.getMessage(bodyKey, arguments, locale));
        notification.setDataJson(dataJson);

        if (user.getFcmToken() == null || !pushAllowed(user, type)) {
            notification.setPushStatus(PushStatus.SKIPPED);
        }

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created [{}] for user {}", type, userId);

        eventPublisher.publishEvent(new NotificationCreatedEvent(this, saved.getId()));

        return saved;

    }


  
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(String email, List<NotificationType> types,
                                                  int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        User user = userFacade.requireByEmail(email);

        if (types == null || types.isEmpty()) {
            return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                    .map(this::toDTO);
        }

        return notificationRepository.findByUserIdAndTypeInOrderByCreatedAtDesc(user.getId(), types, pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public UnreadCountDTO getUnreadCount(String email) {
        User user = userFacade.requireByEmail(email);

        long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        return new UnreadCountDTO(count);
    }


    @Transactional
    public void markAsRead(Long notificationId, String email) {
        User user = userFacade.requireByEmail(email);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("notification.notFound"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("notification.notFound");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = userFacade.requireByEmail(email);

        int updated = notificationRepository.markAllAsReadByUserId(user.getId());
        log.info("Marked {} notifications as read for user {}", updated, user.getId());
    }

    private boolean pushAllowed(User user, NotificationType type) {
        boolean enabled = switch (type) {
            case MATCH_FOUND -> user.isPushMatches();
            case SAVED_SEARCH_MATCH -> user.isPushSavedSearches();
            case CHALLENGE_CREATED, CLAIM_SUBMITTED, CLAIM_APPROVED, CLAIM_DECLINED -> user.isPushVerification();
            case REPORT_EXPIRING, REPORT_EXPIRED, REPORT_RESOLVED -> user.isPushReportUpdates();
            case ACCOUNT_BLOCKED, ACCOUNT_PARTIALLY_BLOCKED -> true;
        };
        if (!enabled) {
            return false;
        }
        Integer dailyLimit = DAILY_PUSH_LIMITS.get(type);
        return dailyLimit == null || notificationRepository.countByUserIdAndTypeAndCreatedAtAfter(
                user.getId(), type, LocalDateTime.now().minusDays(1)) < dailyLimit;
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
