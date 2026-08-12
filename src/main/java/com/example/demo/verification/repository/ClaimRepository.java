package com.example.demo.verification.repository;

import com.example.demo.report.repository.ReportRepository;
import com.example.demo.verification.model.Claim;
import com.example.demo.verification.model.ClaimStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByChallengeIdOrderBySubmittedAtDesc(Long challengeId);

    List<Claim> findByClaimantIdOrderBySubmittedAtDesc(Long claimantId);

    List<Claim> findByChallengeIdAndStatus(Long challengeId, ClaimStatus status);

    List<Claim> findByChallengeIdAndClaimantIdOrderBySubmittedAtDesc(Long challengeId, Long claimantId);

    long countByChallengeIdAndClaimantId(Long challengeId, Long claimantId);

    boolean existsByChallengeIdAndClaimantIdAndStatus(Long challengeId, Long claimantId, ClaimStatus status);

    @Query("SELECT COUNT(c) > 0 FROM Claim c WHERE c.challenge.report.id = :reportId "
            + "AND c.claimant.id = :claimantId AND c.status = :status")
    boolean existsClaimOnReportWithStatus(@Param("reportId") Long reportId,
                                          @Param("claimantId") Long claimantId,
                                          @Param("status") ClaimStatus status);

    @Query("SELECT COUNT(c) FROM Claim c WHERE c.claimant.id = :claimantId AND c.submittedAt >= :since")
    long countByClaimantIdSince(@Param("claimantId") Long claimantId, @Param("since") LocalDateTime since);

    @Query("SELECT c FROM Claim c WHERE c.challenge.report.id = :reportId AND c.status = :status")
    List<Claim> findByReportIdAndStatus(@Param("reportId") Long reportId, @Param("status") ClaimStatus status);

    @Query("SELECT COUNT(c) > 0 FROM Claim c WHERE c.challenge.report.id = :reportId AND c.status = :status")
    boolean existsByReportIdAndStatus(@Param("reportId") Long reportId, @Param("status") ClaimStatus status);

    /**
     * The listing id a claim belongs to, WITHOUT loading any entity — so the listing can be locked
     * before the claim is touched. See ReportRepository.findByIdForUpdate for why the order matters.
     */
    @Query("SELECT c.challenge.report.id FROM Claim c WHERE c.id = :claimId")
    Optional<Long> findReportIdByClaimId(@Param("claimId") Long claimId);

    List<Claim> findByStatusOrderBySubmittedAtDesc(ClaimStatus status);

    List<Claim> findAllByOrderBySubmittedAtDesc();
}
