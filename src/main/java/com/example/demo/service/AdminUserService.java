package com.example.demo.service;

import com.example.demo.dto.UserDetailsDTO;
import com.example.demo.dto.UserListDTO;
import com.example.demo.exception.InvalidAbuseReportException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Claim;
import com.example.demo.model.ClaimStatus;
import com.example.demo.model.NotificationType;
import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.model.UserStatus;
import com.example.demo.repository.ClaimRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ClaimRepository claimRepository;
    private final AccountDeletionService accountDeletionService;
    private final AbuseReportService abuseReportService;
    private final ClaimService claimService;
    private final NotificationService notificationService;

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

        hideActiveReports(user.getId());
        declineOwnPendingClaims(user.getId());
        abuseReportService.resolveReportsForUser(user.getId(), adminEmail);

        notificationService.createNotification(user.getId(), NotificationType.ACCOUNT_BLOCKED,
                "Account blocked",
                "Your account has been blocked for violating the community rules.", "{}");
    }

    @Transactional
    public void partialBlockUser(Long id, String adminEmail) {
        User user = requireNonAdmin(id);
        user.setStatus(UserStatus.PARTIALLY_BLOCKED);
        userRepository.save(user);

        abuseReportService.resolveReportsForUser(user.getId(), adminEmail);

        notificationService.createNotification(user.getId(), NotificationType.ACCOUNT_PARTIALLY_BLOCKED,
                "Account restricted",
                "Your account is restricted: you can no longer post found items or send verification questions.", "{}");
    }

    /**
     * Skida blokadu sa naloga i vraca oglase koje je blokada sakrila.
     *
     * Dve popravke u odnosu na raniju verziju. Prvo, nije bilo provere statusa, pa je unblock
     * nad obrisanim nalogom vracao DELETED u ACTIVE i tiho ponistavao GDPR brisanje. Drugo,
     * blockUser prebacuje sve aktivne oglase korisnika u FLAGGED, a unblock ih nije vracao —
     * korisnik bi se odblokirao, video svoje oglase u "moji oglasi", a njih niko drugi ne bi
     * video niti bi ulazili u matching, bez ijedne poruke i bez nacina da to sam popravi.
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
        restoreHiddenReports(user.getId());
        abuseReportService.dismissReportsForUser(user.getId(), adminEmail);
    }

    private void restoreHiddenReports(Long userId) {
        for (Report report : reportRepository.findByUserId(userId)) {
            if (report.isHiddenByUserBlock()) {
                if (report.getStatus() == ReportStatus.FLAGGED) {
                    report.setStatus(ReportStatus.ACTIVE);
                }
                report.setHiddenByUserBlock(false);
                reportRepository.save(report);
            }
        }
    }

    private User requireNonAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == UserRole.ADMIN) {
            throw new InvalidAbuseReportException("Administrators cannot be blocked");
        }
        return user;
    }

    private void hideActiveReports(Long userId) {
        for (Report report : reportRepository.findByUserId(userId)) {
            if (report.getStatus() == ReportStatus.ACTIVE) {
                report.setStatus(ReportStatus.FLAGGED);
                report.setHiddenByUserBlock(true);
                reportRepository.save(report);
            }

            // Blokiran korisnik je disabled u MyUserDetailsService, pa vise NIKAD ne moze da
            // odluci o claim-ovima na svojim oglasima. Bez ovoga bi tudji claim ostao PENDING
            // zauvek: podnosiocu bi trajno zauzimao jedan od dva pokusaja i slot u parcijalnom
            // unique indeksu, a on bi bez ijednog objasnjenja gledao "Pending review".
            claimService.declinePendingClaimsForReport(report.getId());
        }
    }

    /**
     * Odbijanje ide kroz ClaimService da bi svaki podnosilac dobio CLAIM_DECLINED notifikaciju.
     * Ranije se status upisivao direktno na entitetu, bez ClaimDecidedEvent — pa se claim
     * korisniku tiho menjao pod rukom.
     */
    private void declineOwnPendingClaims(Long userId) {
        for (Claim claim : claimRepository.findByClaimantIdOrderBySubmittedAtDesc(userId)) {
            if (claim.getStatus() == ClaimStatus.PENDING) {
                claimService.declineClaimOnBlock(claim);
            }
        }
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
