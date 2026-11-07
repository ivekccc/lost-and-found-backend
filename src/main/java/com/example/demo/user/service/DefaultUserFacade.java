package com.example.demo.user.service;

import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.shared.stats.DailyCount;
import com.example.demo.shared.stats.DailyCountView;
import com.example.demo.user.UserFacade;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultUserFacade implements UserFacade {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User requireByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("user.notFound"));
    }

    @Override
    @Transactional(readOnly = true)
    public User requireById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user.notFound"));
    }

    @Override
    @Transactional
    public void savePushToken(String email, String fcmToken) {
        User user = requireByEmail(email);
        userRepository.detachPushTokenFromOtherUsers(fcmToken, user.getId());
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void clearPushToken(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFcmToken(null);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyCount> countRegistrationsPerDay(LocalDateTime from) {
        return userRepository.countRegisteredPerDay(from).stream().map(DailyCountView::toDailyCount).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return email == null ? Optional.empty() : userRepository.findByEmail(email);
    }
}
