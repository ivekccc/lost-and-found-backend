package com.example.demo.user.service;

import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.model.AppLanguage;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserRepository userRepository;

    @Transactional
    public void changeLanguage(String userEmail, AppLanguage language) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("user.notFound"));
        user.setLanguage(language);
    }
}
