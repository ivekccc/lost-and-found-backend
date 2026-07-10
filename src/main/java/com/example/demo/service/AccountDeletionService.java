package com.example.demo.service;

import com.example.demo.dto.DeleteAccountRequestDto;
import com.example.demo.model.Challenge;
import com.example.demo.model.Claim;
import com.example.demo.model.Report;
import com.example.demo.model.ReportImage;
import com.example.demo.model.User;
import com.example.demo.model.UserStatus;
import com.example.demo.repository.ChallengeRepository;
import com.example.demo.repository.ClaimRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.event.UserDeletedEvent;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.UserRepository;
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
    private final ReportRepository reportRepository;
    private final ChallengeRepository challengeRepository;
    private final ClaimRepository claimRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
        notificationRepository.deleteByUserId(userId);
        if (user.getAvatarPublicId() != null) {
            imagesToRemove.add(user.getAvatarPublicId());
        }
        stripOwnClaimsOnOtherReports(userId, imagesToRemove);
        deleteOwnReports(userId, imagesToRemove);
        anonymize(user);

        // Cloudinary cleanup is an external side effect: hand it to an AFTER_COMMIT listener so a
        // Cloudinary failure can never roll back the erasure, and orphaned images are removed only
        // once the deletion actually commits.
        eventPublisher.publishEvent(new UserDeletedEvent(this, userId, new ArrayList<>(imagesToRemove)));
    }

    private void stripOwnClaimsOnOtherReports(Long userId, List<String> imagesToRemove) {
        for (Claim claim : claimRepository.findByClaimantIdOrderBySubmittedAtDesc(userId)) {
            if (claim.getPhotoPublicId() != null) {
                imagesToRemove.add(claim.getPhotoPublicId());
            }
            claim.setPhotoUrl(null);
            claim.setPhotoPublicId(null);
            claim.setMessage(null);
            claimRepository.save(claim);
        }
    }

    private void deleteOwnReports(Long userId, List<String> imagesToRemove) {
        for (Report report : reportRepository.findByUserId(userId)) {
            for (Challenge challenge : challengeRepository.findByReportIdOrderByCreatedAtAsc(report.getId())) {
                for (Claim claim : claimRepository.findByChallengeIdOrderBySubmittedAtDesc(challenge.getId())) {
                    if (claim.getPhotoPublicId() != null) {
                        imagesToRemove.add(claim.getPhotoPublicId());
                    }
                    claimRepository.delete(claim);
                }
                challengeRepository.delete(challenge);
            }
            for (ReportImage image : report.getImages()) {
                if (image.getPublicId() != null) {
                    imagesToRemove.add(image.getPublicId());
                }
            }
            reportRepository.delete(report);
        }
    }

    private void anonymize(User user) {
        Long userId = user.getId();
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
