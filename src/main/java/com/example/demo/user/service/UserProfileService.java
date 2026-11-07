package com.example.demo.user.service;

import com.example.demo.location.LocationFacade;
import com.example.demo.messaging.MessagingFacade;
import com.example.demo.user.dto.UserProfileDTO;
import com.example.demo.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final LocationFacade locationFacade;
    private final MessagingFacade messagingFacade;

    @Transactional(readOnly = true)
    public UserProfileDTO toProfile(User user) {
        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getGoogleSub() != null,
                user.getCreatedAt(),
                user.getRole().name(),
                locationFacade.toCityDto(user.getActiveCity()),
                user.getLanguage(),
                messagingFacade.countThanksReceived(user.getId())
        );
    }
}
