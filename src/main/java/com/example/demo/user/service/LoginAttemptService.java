package com.example.demo.user.service;

import com.example.demo.shared.concurrency.AdvisoryLocks;
import com.example.demo.shared.concurrency.LockScope;
import com.example.demo.shared.exception.RateLimitExceededException;
import com.example.demo.user.model.LoginFailure;
import com.example.demo.user.repository.LoginFailureRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final LoginFailureRepository loginFailureRepository;
    private final AdvisoryLocks advisoryLocks;

    @Transactional(propagation = Propagation.MANDATORY)
    public void ensureAllowed(String email) {
        String key = normalize(email);
        advisoryLocks.lock(LockScope.LOGIN, key);
        LocalDateTime windowStart = LocalDateTime.now().minus(WINDOW);
        if (loginFailureRepository.countByEmailAndCreatedAtAfter(key, windowStart) >= MAX_FAILURES) {
            LocalDateTime oldest = loginFailureRepository.findOldestSince(key, windowStart);
            long retryAfter = oldest == null ? WINDOW.toSeconds()
                    : Math.max(1, Duration.between(LocalDateTime.now(), oldest.plus(WINDOW)).toSeconds());
            throw new RateLimitExceededException("auth.tooManyAttempts", retryAfter);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordFailure(String email) {
        LoginFailure failure = new LoginFailure();
        failure.setEmail(normalize(email));
        failure.setCreatedAt(LocalDateTime.now());
        loginFailureRepository.save(failure);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void clear(String email) {
        loginFailureRepository.deleteByEmail(normalize(email));
    }

    @Transactional
    public int purgeExpired() {
        return loginFailureRepository.deleteOlderThan(LocalDateTime.now().minus(WINDOW));
    }

    private String normalize(String email) {
        return email == null ? "" : EmailAddresses.normalize(email);
    }
}
