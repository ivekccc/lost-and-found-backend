package com.example.demo.verification.service;

import com.example.demo.report.model.Report;
import com.example.demo.user.model.User;
import com.example.demo.verification.VerificationFacade;
import com.example.demo.verification.dto.ChallengeQuestionRequestDto;
import com.example.demo.verification.dto.QuestionTemplateDto;
import com.example.demo.verification.model.Challenge;
import com.example.demo.verification.model.Claim;
import com.example.demo.verification.model.ClaimStatus;
import com.example.demo.verification.repository.ChallengeRepository;
import com.example.demo.verification.repository.ClaimRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultVerificationFacade implements VerificationFacade {

    private static final String ERASED_ANSWER = "";

    private final ClaimRepository claimRepository;
    private final ChallengeRepository challengeRepository;
    private final ClaimService claimService;
    private final ChallengeService challengeService;
    private final QuestionTemplateService questionTemplateService;

    @Override
    @Transactional(readOnly = true)
    public boolean isContactRevealedTo(Long reportId, Long viewerId) {
        return claimRepository.existsClaimOnReportWithStatus(reportId, viewerId, ClaimStatus.APPROVED);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasClaimWithStatus(Long reportId, ClaimStatus status) {
        return claimRepository.existsByReportIdAndStatus(reportId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Claim> findClaimsOnReport(Long reportId, ClaimStatus status) {
        return claimRepository.findByReportIdAndStatus(reportId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findChallengeIdBy(Long reportId, Long authorId) {
        return challengeRepository.findByReportIdAndAuthorId(reportId, authorId).map(Challenge::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Claim> findLatestClaim(Long challengeId, Long claimantId) {
        return claimRepository
                .findByChallengeIdAndClaimantIdOrderBySubmittedAtDesc(challengeId, claimantId)
                .stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public long countClaimsBy(Long challengeId, Long claimantId) {
        return claimRepository.countByChallengeIdAndClaimantId(challengeId, claimantId);
    }

    @Override
    public int maxClaimAttemptsPerChallenge() {
        return ClaimService.MAX_ATTEMPTS_PER_CHALLENGE;
    }

    @Override
    @Transactional
    public void createChallengeForListing(Report report, User author, List<ChallengeQuestionRequestDto> questions) {
        challengeService.createChallenge(report, author, questions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionTemplateDto> findActiveTemplatesForCategory(Long categoryId) {
        return questionTemplateService.getActiveTemplatesForCategory(categoryId);
    }

    @Override
    @Transactional
    public void declinePendingClaimsForListing(Long reportId) {
        claimService.declinePendingClaimsForReport(reportId);
    }

    @Override
    @Transactional
    public void declinePendingClaimsForListings(List<Long> reportIds) {
        reportIds.forEach(claimService::declinePendingClaimsForReport);
    }

    @Override
    @Transactional
    public void declinePendingClaimsOfUser(Long userId) {
        // Goes through ClaimService so every claimant gets the CLAIM_DECLINED notification;
        // writing the status straight onto the entity would change it under them silently.
        for (Claim claim : claimRepository.findByClaimantIdOrderBySubmittedAtDesc(userId)) {
            if (claim.getStatus() == ClaimStatus.PENDING) {
                claimService.declineClaimOnBlock(claim);
            }
        }
    }

    @Override
    @Transactional
    public List<String> anonymizeClaimsOfUser(Long userId) {
        List<String> imagesToRemove = new ArrayList<>();
        for (Claim claim : claimRepository.findByClaimantIdOrderBySubmittedAtDesc(userId)) {
            if (claim.getPhotoPublicId() != null) {
                imagesToRemove.add(claim.getPhotoPublicId());
            }
            claim.setPhotoUrl(null);
            claim.setPhotoPublicId(null);
            claim.setMessage(null);
            claim.getAnswers().forEach(answer -> answer.setAnswerText(ERASED_ANSWER));
            claimRepository.save(claim);
        }
        return imagesToRemove;
    }

    @Override
    @Transactional
    public List<String> deleteVerificationDataForListingsOf(Long userId) {
        return deleteChallenges(challengeRepository.findByReportOwner(userId));
    }

    @Override
    @Transactional
    public List<String> deleteVerificationDataForListing(Long reportId) {
        return deleteChallenges(challengeRepository.findByReportIdOrderByCreatedAtAsc(reportId));
    }

    @Override
    @Transactional
    public List<String> scrubClaimsForListing(Long reportId) {
        List<String> photos = new ArrayList<>();
        for (Challenge challenge : challengeRepository.findByReportIdOrderByCreatedAtAsc(reportId)) {
            for (Claim claim : claimRepository.findByChallengeIdOrderBySubmittedAtDesc(challenge.getId())) {
                if (claim.getPhotoPublicId() != null) {
                    photos.add(claim.getPhotoPublicId());
                }
                claim.setPhotoPublicId(null);
                claim.setPhotoUrl(null);
                claim.setMessage(null);
                claim.getAnswers().forEach(answer -> answer.setAnswerText(ERASED_ANSWER));
            }
        }
        return photos;
    }

    private List<String> deleteChallenges(List<Challenge> challenges) {
        List<String> imagesToRemove = new ArrayList<>();
        for (Challenge challenge : challenges) {
            for (Claim claim : claimRepository.findByChallengeIdOrderBySubmittedAtDesc(challenge.getId())) {
                if (claim.getPhotoPublicId() != null) {
                    imagesToRemove.add(claim.getPhotoPublicId());
                }
                claimRepository.delete(claim);
            }
            challengeRepository.delete(challenge);
        }
        return imagesToRemove;
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingClaims() {
        return claimRepository.countByStatus(ClaimStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> oldestPendingClaimAt() {
        return Optional.ofNullable(claimRepository.findOldestSubmittedAt(ClaimStatus.PENDING));
    }
}
