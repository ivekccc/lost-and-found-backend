package com.example.demo.user.admin;

import com.example.demo.notification.model.NotificationType;
import com.example.demo.notification.NotificationFacade;
import com.example.demo.report.ReportFacade;
import com.example.demo.shared.exception.InvalidAbuseReportException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.dto.UserDetailsDTO;
import com.example.demo.user.dto.UserListDTO;
import com.example.demo.user.model.User;
import com.example.demo.user.model.UserRole;
import com.example.demo.user.model.UserStatus;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.user.service.AccountDeletionService;
import com.example.demo.verification.VerificationFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final ReportFacade reportFacade;
    private final VerificationFacade verificationFacade;
    private final AccountDeletionService accountDeletionService;
    private final NotificationFacade notificationFacade;

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getStatus() != UserStatus.DELETED) {
            accountDeletionService.deleteAccount(user);
        }
    }

    @Transactional
    public void blockUser(Long id, String adminEmail) {
        User user = requireNonAdmin(id);
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);

        verificationFacade.declinePendingClaimsForListings(
                reportFacade.hideListingsOfBlockedUser(user.getId()));
        verificationFacade.declinePendingClaimsOfUser(user.getId());
        reportFacade.resolveAbuseReportsForUser(user.getId(), adminEmail);

        notificationFacade.notify(user.getId(), NotificationType.ACCOUNT_BLOCKED,
                "Account blocked",
                "Your account has been blocked for violating the community rules.", "{}");
    }

    @Transactional
    public void partialBlockUser(Long id, String adminEmail) {
        User user = requireNonAdmin(id);
        user.setStatus(UserStatus.PARTIALLY_BLOCKED);
        userRepository.save(user);

        reportFacade.resolveAbuseReportsForUser(user.getId(), adminEmail);

        notificationFacade.notify(user.getId(), NotificationType.ACCOUNT_PARTIALLY_BLOCKED,
                "Account restricted",
                "Your account is restricted: you can no longer post found items or send verification questions.", "{}");
    }

    /**
     * Lifts a block and restores what the block itself hid — nothing more. A listing flagged by
     * moderation before the block stays flagged, which is why the block records what it hid instead
     * of guessing from the status.
     */
    @Transactional
    public void unblockUser(Long id, String adminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.BLOCKED && user.getStatus() != UserStatus.PARTIALLY_BLOCKED) {
            throw new IllegalArgumentException("Only blocked accounts can be unblocked");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        reportFacade.restoreListingsHiddenByBlock(user.getId());
        reportFacade.dismissAbuseReportsForUser(user.getId(), adminEmail);
    }

    private User requireNonAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == UserRole.ADMIN) {
            throw new InvalidAbuseReportException("Administrators cannot be blocked");
        }
        return user;
    }


    public List<UserListDTO> getAllUsers(UserRole role) {
        List<User> users;

        if (role != null) {
            users = userRepository.findByRole(role);
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
                .map(this::mapToListDTO)
                .collect(Collectors.toList());
    }

    public UserDetailsDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDetailsDTO(user);
    }

    private UserListDTO mapToListDTO(User user) {
        return new UserListDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    private UserDetailsDTO mapToDetailsDTO(User user) {
        return new UserDetailsDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
