package com.example.demo.user.service;

import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.dto.NotificationSettingsDto;
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
        requireUser(userEmail).setLanguage(language);
    }

    @Transactional(readOnly = true)
    public NotificationSettingsDto getNotificationSettings(String userEmail) {
        return toSettingsDto(requireUser(userEmail));
    }

    @Transactional
    public NotificationSettingsDto updateNotificationSettings(String userEmail, NotificationSettingsDto settings) {
        User user = requireUser(userEmail);
        user.setPushMatches(settings.getMatches());
        user.setPushSavedSearches(settings.getSavedSearches());
        user.setPushVerification(settings.getVerification());
        user.setPushReportUpdates(settings.getReportUpdates());
        user.setPushMessages(settings.getMessages());
        return toSettingsDto(user);
    }

    private User requireUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("user.notFound"));
    }

    private NotificationSettingsDto toSettingsDto(User user) {
        return new NotificationSettingsDto(
                user.isPushMatches(),
                user.isPushSavedSearches(),
                user.isPushVerification(),
                user.isPushReportUpdates(),
                user.isPushMessages());
    }
}
