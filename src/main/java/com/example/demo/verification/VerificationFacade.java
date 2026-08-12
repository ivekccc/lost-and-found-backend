package com.example.demo.verification;

import com.example.demo.report.model.Report;
import com.example.demo.user.model.User;
import com.example.demo.verification.dto.ChallengeQuestionRequestDto;
import com.example.demo.verification.dto.QuestionTemplateDto;
import com.example.demo.verification.model.Claim;
import com.example.demo.verification.model.ClaimStatus;

import java.util.List;
import java.util.Optional;

/**
 * The only entry point other modules may use to reach challenges and claims.
 *
 * Cleanup of a user's verification history lives here rather than in the caller, because the
 * order in which claims and challenges go away is a rule of this module: claims reference
 * challenges, and neither has a cascading delete.
 */
public interface VerificationFacade {

    boolean isContactRevealedTo(Long reportId, Long viewerId);

    boolean hasClaimWithStatus(Long reportId, ClaimStatus status);

    List<Claim> findClaimsOnReport(Long reportId, ClaimStatus status);

    Optional<Long> findChallengeIdBy(Long reportId, Long authorId);

    Optional<Claim> findLatestClaim(Long challengeId, Long claimantId);

    /** How many attempts the viewer has already spent on this challenge. */
    long countClaimsBy(Long challengeId, Long claimantId);

    /** How many attempts one claimant gets per challenge — the client needs it to stop offering the action. */
    int maxClaimAttemptsPerChallenge();

    void createChallengeForListing(Report report, User author, List<ChallengeQuestionRequestDto> questions);

    List<QuestionTemplateDto> findActiveTemplatesForCategory(Long categoryId);

    void declinePendingClaimsForListing(Long reportId);

    void declinePendingClaimsForListings(List<Long> reportIds);

    void declinePendingClaimsOfUser(Long userId);

    /** Strips photos and messages from the user's claims; returns the Cloudinary ids to remove. */
    List<String> anonymizeClaimsOfUser(Long userId);

    /** Removes the challenges and claims attached to a user's listings, before those listings go. */
    List<String> deleteVerificationDataForListingsOf(Long userId);
}
