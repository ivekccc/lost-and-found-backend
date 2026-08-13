package com.example.demo.user.service;

import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.UserFacade;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultUserFacade implements UserFacade {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User requireByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public User requireById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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
}
