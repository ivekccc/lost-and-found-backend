package com.example.demo.user.service;

import com.example.demo.user.repository.PasswordResetRepository;
import com.example.demo.user.repository.PreRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupTasks {

    private final PreRegistrationRepository preRegistrationRepository;
    private final PasswordResetRepository passwordResetRepository;

    @Scheduled(fixedRateString = "${app.cleanup.interval-ms:3600000}")
    @Transactional
    public void cleanupExpiredPreRegistrations() {
        LocalDateTime now = LocalDateTime.now();
        int deletedPreRegistrations = preRegistrationRepository.deleteByExpiresAtBefore(now);
        int deletedPasswordResets = passwordResetRepository.deleteByExpiresAtBefore(now);

        if (deletedPreRegistrations > 0) {
            log.info("Cleaned up {} expired pre-registrations", deletedPreRegistrations);
        }
        if (deletedPasswordResets > 0) {
            log.info("Cleaned up {} expired password resets", deletedPasswordResets);
        }
    }
}
