package com.example.demo.user.service;

import com.example.demo.messaging.MessagingFacade;
import com.example.demo.notification.NotificationFacade;
import com.example.demo.report.ReportFacade;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.dto.DeleteAccountRequestDto;
import com.example.demo.user.event.UserDeletedEvent;
import com.example.demo.user.model.User;
import com.example.demo.user.model.UserStatus;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.verification.VerificationFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final ReportFacade reportFacade;
    private final VerificationFacade verificationFacade;
    private final MessagingFacade messagingFacade;
    
    private final NotificationFacade notificationFacade;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Self-service deletion: the caller must re-confirm their identity before the account is erased —
     * with the current password, or with a fresh Google ID token for Google-linked accounts
     * (which have no usable password).
     */
    @Transactional
    public void deleteOwnAccount(String userEmail, DeleteAccountRequestDto request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("user.notFound"));

        verifyIdentity(user, request);
        deleteAccount(user);
    }

    private void verifyIdentity(User user, DeleteAccountRequestDto request) {
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BadCredentialsException("Invalid password");
            }
            return;
        }
        if (request.getGoogleIdToken() != null && !request.getGoogleIdToken().isBlank()) {
            GoogleTokenVerifierService.GoogleIdentity identity =
                    googleTokenVerifierService.verify(request.getGoogleIdToken());
            if (user.getGoogleSub() == null || !identity.subject().equals(user.getGoogleSub())) {
                throw new BadCredentialsException("Google account does not match this account");
            }
            return;
        }
        throw new BadCredentialsException("Password or Google confirmation is required");
    }

    /**
     * GDPR erasure by anonymization. The user's personal identity is stripped and the account
     * becomes an inactive tombstone so foreign keys stay valid, while content entangled with other
     * users (claims and challenges on their reports) survives without any of this user's personal data.
     */
    @Transactional
    public void deleteAccount(User user) {
        Long userId = user.getId();
        List<String> imagesToRemove = new ArrayList<>();

        // The bulk notification delete clears the persistence context, so it must run before any
        // entity changes below — otherwise those pending changes would be discarded on the clear.
        notificationFacade.deleteNotificationsOfUser(userId);
        messagingFacade.deleteConversationsOfUser(userId);
        if (user.getAvatarPublicId() != null) {
            imagesToRemove.add(user.getAvatarPublicId());
        }
        imagesToRemove.addAll(verificationFacade.anonymizeClaimsOfUser(userId));
        // Verification data first: claims reference challenges and challenges reference listings,
        // and none of those foreign keys cascade.
        imagesToRemove.addAll(verificationFacade.deleteVerificationDataForListingsOf(userId));
        reportFacade.deleteSavedListingsOfUser(userId);
        reportFacade.deleteSavedSearchesOfUser(userId);
        imagesToRemove.addAll(reportFacade.deleteListingsOfUser(userId));
        anonymize(user);

        // Cloudinary cleanup is an external side effect: hand it to an AFTER_COMMIT listener so a
        // Cloudinary failure can never roll back the erasure, and orphaned images are removed only
        // once the deletion actually commits.
        eventPublisher.publishEvent(new UserDeletedEvent(this, userId, new ArrayList<>(imagesToRemove)));
    }

    private void anonymize(User user) {
        Long userId = user.getId();
        refreshTokenService.endAllSessions(userId, true);
        user.setEmail("deleted-" + userId + "@deleted.local");
        user.setUsername("deleted-" + userId);
        user.setFirstName(null);
        user.setLastName(null);
        user.setPhoneNumber(null);
        user.setFcmToken(null);
        user.setGoogleSub(null);
        user.setAvatarUrl(null);
        user.setAvatarPublicId(null);
        user.setPassword("");
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }
}
