package com.example.demo.repository;

import com.example.demo.model.Claim;
import com.example.demo.model.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByChallengeIdOrderBySubmittedAtDesc(Long challengeId);

    List<Claim> findByClaimantIdOrderBySubmittedAtDesc(Long claimantId);

    List<Claim> findByChallengeIdAndStatus(Long challengeId, ClaimStatus status);

    long countByChallengeIdAndClaimantId(Long challengeId, Long claimantId);

    boolean existsByChallengeIdAndClaimantIdAndStatus(Long challengeId, Long claimantId, ClaimStatus status);

    @Query("SELECT COUNT(c) FROM Claim c WHERE c.claimant.id = :claimantId AND c.submittedAt >= :since")
    long countByClaimantIdSince(@Param("claimantId") Long claimantId, @Param("since") LocalDateTime since);

    @Query("SELECT c FROM Claim c WHERE c.challenge.report.id = :reportId AND c.status = :status")
    List<Claim> findByReportIdAndStatus(@Param("reportId") Long reportId, @Param("status") ClaimStatus status);

    List<Claim> findByStatusOrderBySubmittedAtDesc(ClaimStatus status);

    List<Claim> findAllByOrderBySubmittedAtDesc();
}
