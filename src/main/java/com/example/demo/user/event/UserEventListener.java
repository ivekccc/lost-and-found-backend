package com.example.demo.user.event;

import com.example.demo.notification.NotificationFacade;
import com.example.demo.shared.media.CloudinaryService;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final NotificationFacade notificationFacade;

    @Async
    @TransactionalEventListener
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        try {
            notificationFacade.sendPasswordResetEmail(event.email(), event.code(), event.locale());
        } catch (RuntimeException e) {
            log.error("Password reset email could not be sent", e);
        }
    }

    @TransactionalEventListener
    public void onUserDeleted(UserDeletedEvent event) {
        log.info("UserDeletedEvent received for user {} ({} images to remove)",
                event.getUserId(), event.getImagePublicIds().size());
        cloudinaryService.deleteImages(event.getImagePublicIds());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onGoogleAvatarRequested(GoogleAvatarRequestedEvent event) {
        User user = userRepository.findById(event.getUserId()).orElse(null);
        if (user == null || user.getAvatarUrl() != null) {
            return;
        }

        CloudinaryService.UploadedImage uploaded =
                cloudinaryService.uploadImageFromUrl(event.getPictureUrl());
        if (uploaded == null) {
            log.warn("Google avatar upload failed for user {}", event.getUserId());
            return;
        }

        user.setAvatarUrl(uploaded.url());
        user.setAvatarPublicId(uploaded.publicId());
        userRepository.save(user);
    }
}
