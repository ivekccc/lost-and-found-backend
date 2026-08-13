package com.example.demo.user.service;

import com.example.demo.user.model.RefreshToken;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void register(User user, String refreshToken, LocalDateTime expiresAt) {
        RefreshToken session = new RefreshToken();
        session.setUser(user);
        session.setTokenHash(hash(refreshToken));
        session.setExpiresAt(expiresAt);
        refreshTokenRepository.save(session);
    }

    @Transactional
    public boolean consume(String refreshToken) {
        return refreshTokenRepository.deleteLiveByTokenHash(hash(refreshToken), LocalDateTime.now()) > 0;
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
