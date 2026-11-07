package com.example.demo.user.service;

import com.example.demo.user.event.UserSessionsEndedEvent;
import com.example.demo.user.model.RefreshToken;
import com.example.demo.user.model.User;
import com.example.demo.user.model.UserRole;
import com.example.demo.user.repository.RefreshTokenRepository;
import com.example.demo.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Duration REUSE_GRACE = Duration.ofSeconds(30);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${jwt.refresh-token.user-session-lifetime-days}")
    private long userSessionLifetimeDays;

    @Value("${jwt.refresh-token.admin-session-lifetime-hours}")
    private long adminSessionLifetimeHours;

    @Transactional
    public void startSession(User user, String refreshToken, LocalDateTime expiresAt) {
        LocalDateTime familyExpiresAt = LocalDateTime.now().plus(sessionLifetime(user));
        save(user, refreshToken, expiresAt, UUID.randomUUID(), familyExpiresAt);
    }

    @Transactional
    public Optional<RotatedSession> rotate(String refreshToken) {
        Optional<RefreshToken> found = refreshTokenRepository.findForRotation(hash(refreshToken));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken current = found.get();
        LocalDateTime now = LocalDateTime.now();
        if (current.getConsumedAt() != null) {
            if (current.getConsumedAt().plus(REUSE_GRACE).isBefore(now)) {
                endAllSessions(current.getUser().getId(), true);
            }
            return Optional.empty();
        }
        if (!current.getExpiresAt().isAfter(now)
                || !current.getFamilyExpiresAt().isAfter(now)
                || current.getCredentialsVersion() != current.getUser().getTokenVersion()) {
            refreshTokenRepository.deleteFamily(current.getFamilyId());
            return Optional.empty();
        }
        current.setConsumedAt(now);
        return Optional.of(new RotatedSession(current.getUser(), current.getFamilyId(), current.getFamilyExpiresAt()));
    }

    @Transactional
    public void continueSession(RotatedSession session, String refreshToken, LocalDateTime expiresAt) {
        LocalDateTime cappedExpiry = expiresAt.isAfter(session.familyExpiresAt()) ? session.familyExpiresAt() : expiresAt;
        save(session.user(), refreshToken, cappedExpiry, session.familyId(), session.familyExpiresAt());
    }

    @Transactional
    public Optional<User> endSession(String refreshToken) {
        return refreshTokenRepository.findForRotation(hash(refreshToken))
                .map(token -> {
                    refreshTokenRepository.deleteFamily(token.getFamilyId());
                    return token.getUser();
                });
    }

    @Transactional(readOnly = true)
    public long countActiveSessions(Long userId) {
        return refreshTokenRepository.countActiveSessions(userId, LocalDateTime.now());
    }

    @Transactional
    public void endAllSessions(Long userId, boolean forgetDevice) {
        refreshTokenRepository.deleteByUserId(userId);
        userRepository.findById(userId).ifPresent(user -> {
            user.setTokenVersion(user.getTokenVersion() + 1);
            if (forgetDevice) {
                user.setFcmToken(null);
            }
            eventPublisher.publishEvent(new UserSessionsEndedEvent(userId, user.getEmail()));
        });
    }

    @Transactional
    public int purgeExpired() {
        return refreshTokenRepository.deleteExpired(LocalDateTime.now());
    }

    private void save(User user, String refreshToken, LocalDateTime expiresAt, UUID familyId, LocalDateTime familyExpiresAt) {
        RefreshToken session = new RefreshToken();
        session.setUser(user);
        session.setTokenHash(hash(refreshToken));
        session.setExpiresAt(expiresAt);
        session.setFamilyId(familyId);
        session.setFamilyExpiresAt(familyExpiresAt);
        session.setCredentialsVersion(user.getTokenVersion());
        refreshTokenRepository.save(session);
    }

    private Duration sessionLifetime(User user) {
        return user.getRole() == UserRole.ADMIN
                ? Duration.ofHours(adminSessionLifetimeHours)
                : Duration.ofDays(userSessionLifetimeDays);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record RotatedSession(User user, UUID familyId, LocalDateTime familyExpiresAt) {
    }
}
