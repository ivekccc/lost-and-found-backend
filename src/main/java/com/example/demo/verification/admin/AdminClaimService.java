package com.example.demo.verification.admin;

import com.example.demo.user.model.User;
import com.example.demo.verification.model.Claim;
import com.example.demo.verification.model.ClaimStatus;
import com.example.demo.verification.repository.ClaimRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@RequiredArgsConstructor
public class AdminClaimService {

    private final ClaimRepository claimRepository;

    @Transactional(readOnly = true)
    public List<AdminClaimListDto> getClaims(ClaimStatus status) {
        List<Claim> claims = status != null
                ? claimRepository.findByStatusOrderBySubmittedAtDesc(status)
                : claimRepository.findAllByOrderBySubmittedAtDesc();

        return claims.stream().map(this::toDto).toList();
    }



    private AdminClaimListDto toDto(Claim claim) {
        return new AdminClaimListDto(
                claim.getId(),
                claim.getChallenge().getReport().getId(),
                claim.getChallenge().getReport().getTitle(),
                claim.getChallenge().getReport().getType(),
                claim.getChallenge().getId(),
                buildDisplayName(claim.getChallenge().getAuthor()),
                buildDisplayName(claim.getClaimant()),
                claim.getStatus(),
                claim.getSubmittedAt(),
                claim.getDecidedAt()
        );
    }

    private String buildDisplayName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String combined = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        String trimmed = combined.trim();
        return trimmed.isEmpty() ? user.getUsername() : trimmed;
    }
}
