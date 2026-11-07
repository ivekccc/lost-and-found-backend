package com.example.demo.user.admin;

import com.example.demo.audit.AdminActionEvent;
import com.example.demo.audit.AuditAction;
import com.example.demo.audit.AuditTargetType;
import com.example.demo.notification.NotificationFacade;
import com.example.demo.notification.model.NotificationType;
import com.example.demo.report.ReportFacade;
import com.example.demo.shared.exception.InvalidAbuseReportException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.shared.web.PageQuery;
import com.example.demo.user.dto.UserDetailsDTO;
import com.example.demo.user.dto.UserListDTO;
import com.example.demo.user.model.User;
import com.example.demo.user.model.UserRole;
import com.example.demo.user.model.UserStatus;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.user.repository.UserSpecifications;
import com.example.demo.user.service.AccountDeletionService;
import com.example.demo.user.service.RefreshTokenService;
import com.example.demo.verification.VerificationFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "createdAt", "createdAt",
            "email", "email",
            "username", "username",
            "lastName", "lastName",
            "status", "status",
            "id", "id");

    private final UserRepository userRepository;
    private final ReportFacade reportFacade;
    private final VerificationFacade verificationFacade;
    private final AccountDeletionService accountDeletionService;
    private final NotificationFacade notificationFacade;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void deleteUser(Long id) {
        User user = requireNonAdmin(id, "admin.user.cannotDeleteAdmin");
        if (user.getStatus() != UserStatus.DELETED) {
            accountDeletionService.deleteAccount(user);
            eventPublisher.publishEvent(AdminActionEvent.of(AuditAction.USER_DELETED, AuditTargetType.USER, id,
                    "Deleted the account of user #" + id));
        }
    }

    @Transactional
    public void blockUser(Long id, String adminEmail) {
        User user = requireNonAdmin(id);
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
        eventPublisher.publishEvent(AdminActionEvent.of(AuditAction.USER_BLOCKED, AuditTargetType.USER, id,
                "Blocked user #" + id));

        verificationFacade.declinePendingClaimsForListings(
                reportFacade.hideListingsOfBlockedUser(user.getId()));
        verificationFacade.declinePendingClaimsOfUser(user.getId());
        reportFacade.resolveAbuseReportsForUser(user.getId(), adminEmail);
        refreshTokenService.revokeAllForUser(user.getId());

        notificationFacade.notify(user.getId(), NotificationType.ACCOUNT_BLOCKED,
                "notification.accountBlocked.title",
                "notification.accountBlocked.body", "{}");
    }

    @Transactional
    public void partialBlockUser(Long id, String adminEmail) {
        User user = requireNonAdmin(id);
        user.setStatus(UserStatus.PARTIALLY_BLOCKED);
        userRepository.save(user);
        eventPublisher.publishEvent(AdminActionEvent.of(AuditAction.USER_RESTRICTED, AuditTargetType.USER, id,
                "Restricted user #" + id));

        reportFacade.resolveAbuseReportsForUser(user.getId(), adminEmail);

        notificationFacade.notify(user.getId(), NotificationType.ACCOUNT_PARTIALLY_BLOCKED,
                "notification.accountRestricted.title",
                "notification.accountRestricted.body", "{}");
    }

    /**
     * Lifts a block and restores what the block itself hid — nothing more. A listing flagged by
     * moderation before the block stays flagged, which is why the block records what it hid instead
     * of guessing from the status.
     */
    @Transactional
    public void unblockUser(Long id, String adminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user.notFound"));

        if (user.getStatus() != UserStatus.BLOCKED && user.getStatus() != UserStatus.PARTIALLY_BLOCKED) {
            throw new IllegalArgumentException("admin.user.notBlocked");
        }
        eventPublisher.publishEvent(new AdminActionEvent(AuditAction.USER_UNBLOCKED, AuditTargetType.USER, id,
                "Lifted restrictions on user #" + id, Map.of("previousStatus", user.getStatus().name())));

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        reportFacade.restoreListingsHiddenByBlock(user.getId());
        reportFacade.dismissAbuseReportsForUser(user.getId(), adminEmail);
    }

    private User requireNonAdmin(Long id) {
        return requireNonAdmin(id, "admin.user.cannotBlockAdmin");
    }

    private User requireNonAdmin(Long id, String messageKey) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user.notFound"));
        if (user.getRole() == UserRole.ADMIN) {
            throw new InvalidAbuseReportException(messageKey);
        }
        return user;
    }

    @Transactional(readOnly = true)
    public Page<UserListDTO> getUsers(UserRole role, UserStatus status, String search,
                                      Integer page, Integer size, String sort, String direction) {
        Specification<User> spec = Specification.allOf(
                UserSpecifications.hasRole(role),
                UserSpecifications.hasStatus(status),
                UserSpecifications.matches(search));
        PageRequest pageRequest = PageQuery.of(page, size, sort, direction, SORTABLE_COLUMNS, "createdAt", Sort.Direction.DESC);
        return userRepository.findAll(spec, pageRequest).map(this::mapToListDTO);
    }

    @Transactional(readOnly = true)
    public UserDetailsDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user.notFound"));
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
                user.getCreatedAt(),
                refreshTokenService.countActiveSessions(user.getId()),
                user.getGoogleSub() != null,
                user.getLanguage(),
                user.getActiveCity() == null ? null : user.getActiveCity().getName()
        );
    }

    @Transactional
    public void revokeSessions(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user.notFound"));
        refreshTokenService.revokeAllForUser(user.getId());
        eventPublisher.publishEvent(AdminActionEvent.of(AuditAction.USER_SESSIONS_REVOKED, AuditTargetType.USER, id,
                "Signed user #" + id + " out of every device"));
    }
}
