package com.example.demo.verification.admin;

import com.example.demo.shared.web.PageQuery;
import com.example.demo.user.model.User;
import com.example.demo.verification.model.Claim;
import com.example.demo.verification.model.ClaimStatus;
import com.example.demo.verification.repository.ClaimRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminClaimService {

    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "submittedAt", "submittedAt",
            "status", "status",
            "id", "id");

    private final ClaimRepository claimRepository;

    @Transactional(readOnly = true)
    public Page<AdminClaimListDto> getClaims(ClaimStatus status, Long claimantId, Long reportId,
                                             Integer page, Integer size, String sort, String direction) {
        PageRequest pageRequest = PageQuery.of(page, size, sort, direction, SORTABLE_COLUMNS, "submittedAt", Sort.Direction.DESC);
        Specification<Claim> spec = Specification.allOf(
                status == null ? Specification.unrestricted() : (root, query, builder) -> builder.equal(root.get("status"), status),
                claimantId == null ? Specification.unrestricted() : (root, query, builder) -> builder.equal(root.get("claimant").get("id"), claimantId),
                reportId == null ? Specification.unrestricted() : (root, query, builder) -> builder.equal(root.get("challenge").get("report").get("id"), reportId));
        return claimRepository.findAll(spec, pageRequest).map(this::toDto);
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
