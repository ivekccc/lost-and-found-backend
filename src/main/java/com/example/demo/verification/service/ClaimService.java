package com.example.demo.verification.service;

import com.example.demo.verification.dto.RevealedContactDto;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;
import com.example.demo.report.ReportFacade;
import com.example.demo.shared.exception.InvalidClaimException;
import com.example.demo.shared.exception.RateLimitExceededException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.model.User;
import com.example.demo.user.UserFacade;
import com.example.demo.verification.dto.ClaimAnswerDto;
import com.example.demo.verification.dto.ClaimAnswerRequestDto;
import com.example.demo.verification.dto.ClaimDetailsDto;
import com.example.demo.verification.dto.ClaimDto;
import com.example.demo.verification.dto.ClaimReviewAnswerDto;
import com.example.demo.verification.dto.ClaimSummaryDto;
import com.example.demo.verification.dto.ClaimantChallengeDto;
import com.example.demo.verification.dto.ClaimantChallengeQuestionDto;
import com.example.demo.verification.dto.CreateClaimRequestDto;
import com.example.demo.verification.event.ClaimDecidedEvent;
import com.example.demo.verification.event.ClaimSubmittedEvent;
import com.example.demo.verification.model.Challenge;
import com.example.demo.verification.model.ChallengeQuestion;
import com.example.demo.verification.model.Claim;
import com.example.demo.verification.model.ClaimAnswer;
import com.example.demo.verification.model.ClaimStatus;
import com.example.demo.verification.model.QuestionKind;
import com.example.demo.verification.repository.ChallengeRepository;
import com.example.demo.verification.repository.ClaimRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class ClaimService {

    private static final int MAX_CLAIMS_PER_DAY = 5;
    public static final int MAX_ATTEMPTS_PER_CHALLENGE = 2;
    private static final long DAY_IN_SECONDS = 86400;

    private final ClaimRepository claimRepository;
    private final ChallengeRepository challengeRepository;
    private final ReportFacade reportFacade;
    private final UserFacade userFacade;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public ClaimantChallengeDto getChallengeForClaimant(Long challengeId, String userEmail) {
        User user = findUser(userEmail);
        Challenge challenge = findChallenge(challengeId);
        Report report = challenge.getReport();

        if (report.getType() == ReportType.LOST && !isOwnerOrAuthor(report, challenge, user)) {
            throw new InvalidClaimException("claim.onlyOwnerCanAnswer");
        }
  
        if (report.getStatus() != ReportStatus.ACTIVE) {
            throw new ResourceNotFoundException("challenge.notFound");
        }

        List<ClaimantChallengeQuestionDto> questions = challenge.getQuestions().stream()
                .map(question -> new ClaimantChallengeQuestionDto(
                        question.getId(),
                        question.getPrompt(),
                        question.getKind(),
                        question.getChoices(),
                        question.getOrderIndex()
                ))
                .toList();

        return new ClaimantChallengeDto(
                challenge.getId(),
                report.getId(),
                report.getTitle(),
                challenge.getCreatedAt(),
                questions
        );
    }

    @Transactional
    public ClaimDto submitClaim(Long challengeId, String userEmail, CreateClaimRequestDto request) {
        User claimant = findUser(userEmail);
        Challenge challenge = findChallenge(challengeId);
        Report report = challenge.getReport();

        validateClaimant(challenge, report, claimant);
        validateLimits(challengeId, claimant.getId());

        Claim claim = new Claim();
        claim.setChallenge(challenge);
        claim.setClaimant(claimant);
        claim.setMessage(request.getMessage());
        claim.setPhotoUrl(request.getPhotoUrl());
        claim.setPhotoPublicId(request.getPhotoPublicId());

        buildAnswers(claim, challenge, request.getAnswers());

        Claim saved = claimRepository.save(claim);

        eventPublisher.publishEvent(new ClaimSubmittedEvent(
                this, saved.getId(), challenge.getId(), report.getId(),
                challenge.getAuthor().getId(), report.getTitle()));

        return toDto(saved);
    }

    private void validateClaimant(Challenge challenge, Report report, User claimant) {
        if (challenge.getAuthor().getId().equals(claimant.getId())) {
            throw new InvalidClaimException("claim.cannotClaimOwn");
        }
        if (report.getStatus() != ReportStatus.ACTIVE) {
            throw new InvalidClaimException("report.noLongerActive");
        }
        if (report.getType() == ReportType.LOST && !report.getUser().getId().equals(claimant.getId())) {
            throw new InvalidClaimException("claim.onlyOwnerCanAnswer");
        }
    }

    private void validateLimits(Long challengeId, Long claimantId) {
        if (claimRepository.existsByChallengeIdAndClaimantIdAndStatus(challengeId, claimantId, ClaimStatus.PENDING)) {
            throw new InvalidClaimException("claim.alreadyPending");
        }
        // An approved claim used to be blocked only indirectly, through "the listing must be ACTIVE".
        // That guard falls away the moment moderation or expiry moves the listing elsewhere.
        if (claimRepository.existsByChallengeIdAndClaimantIdAndStatus(challengeId, claimantId, ClaimStatus.APPROVED)) {
            throw new InvalidClaimException("claim.alreadyApproved");
        }
        if (claimRepository.countByChallengeIdAndClaimantId(challengeId, claimantId) >= MAX_ATTEMPTS_PER_CHALLENGE) {
            throw new InvalidClaimException("claim.noAttemptsLeft");
        }
        long dailyCount = claimRepository.countByClaimantIdSince(claimantId, LocalDateTime.now().minusDays(1));
        if (dailyCount >= MAX_CLAIMS_PER_DAY) {
            throw new RateLimitExceededException(
                    "claim.dailyLimit", DAY_IN_SECONDS);
        }
    }

    private void buildAnswers(Claim claim, Challenge challenge, List<ClaimAnswerRequestDto> answers) {
        Map<Long, ChallengeQuestion> questionsById = new HashMap<>();
        challenge.getQuestions().forEach(question -> questionsById.put(question.getId(), question));

        Map<Long, ClaimAnswerRequestDto> answersByQuestionId = new HashMap<>();
        for (ClaimAnswerRequestDto answer : answers) {
            if (!questionsById.containsKey(answer.getQuestionId())) {
                throw new InvalidClaimException("claim.questionNotInChallenge");
            }
            if (answersByQuestionId.put(answer.getQuestionId(), answer) != null) {
                throw new InvalidClaimException("claim.duplicateAnswer");
            }
        }
        if (answersByQuestionId.size() != questionsById.size()) {
            throw new InvalidClaimException("claim.allQuestionsRequired");
        }

        for (ChallengeQuestion question : challenge.getQuestions()) {
            ClaimAnswerRequestDto answerRequest = answersByQuestionId.get(question.getId());

            ClaimAnswer answer = new ClaimAnswer();
            answer.setClaim(claim);
            answer.setQuestion(question);
            answer.setAnswerText(answerRequest.getAnswerText());

            if (question.getKind() == QuestionKind.CHOICE) {
                if (question.getChoices() == null || !question.getChoices().contains(answerRequest.getAnswerText())) {
                    throw new InvalidClaimException("claim.answerNotAChoice");
                }
                answer.setIsCorrect(answerRequest.getAnswerText().equals(question.getCorrectAnswer()));
            }

            claim.getAnswers().add(answer);
        }
    }

    @Transactional(readOnly = true)
    public List<ClaimSummaryDto> getClaimsForChallenge(Long challengeId, String userEmail) {
        User user = findUser(userEmail);
        Challenge challenge = findChallenge(challengeId);
        requireChallengeAuthor(challenge, user);

        return claimRepository.findByChallengeIdOrderBySubmittedAtDesc(challengeId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClaimDetailsDto getClaimDetails(Long claimId, String userEmail) {
        User user = findUser(userEmail);
        Claim claim = findClaim(claimId);
        requireChallengeAuthor(claim.getChallenge(), user);

        return toDetailsDto(claim);
    }

    @Transactional(readOnly = true)
    public List<ClaimDto> getMyClaims(String userEmail) {
        User user = findUser(userEmail);
        return claimRepository.findByClaimantIdOrderBySubmittedAtDesc(user.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ClaimDetailsDto approveClaim(Long claimId, String userEmail) {
        User user = findUser(userEmail);

        // The listing is locked BEFORE the claim is loaded — see ReportRepository.findByIdForUpdate.
        // The other order returns a cached instance and the check reads stale state.
        Report report = lockReportOfClaim(claimId);

        Claim claim = findClaim(claimId);
        requireChallengeAuthor(claim.getChallenge(), user);
        requirePending(claim);

        // Without this check, approving a stale claim overwrites ANY listing status with MATCHED,
        // including FLAGGED — an ordinary user undoing a moderation decision.
        if (report.getStatus() != ReportStatus.ACTIVE) {
            throw new InvalidClaimException("report.noLongerActive");
        }

        claim.setStatus(ClaimStatus.APPROVED);
        claim.setDecidedAt(LocalDateTime.now());

        // A listing's status changes only when its own owner closes it. On a found listing the owner
        // wrote the challenge, so approving is their decision; on a lost one the finder is a third
        // party and must not close someone else's listing.
        if (report.getType() == ReportType.FOUND) {
            report.setStatus(ReportStatus.MATCHED);
        }

        for (Claim other : claimRepository.findByReportIdAndStatus(report.getId(), ClaimStatus.PENDING)) {
            if (!other.getId().equals(claim.getId())) {
                other.setStatus(ClaimStatus.DECLINED);
                other.setDecidedAt(LocalDateTime.now());
                publishDecision(other, false);
            }
        }

        publishDecision(claim, true);

        return toDetailsDto(claim);
    }

    @Transactional
    public ClaimDetailsDto declineClaim(Long claimId, String userEmail) {
        User user = findUser(userEmail);

        // Same lock as approveClaim: declining and approving on one listing decide the same resource.
        lockReportOfClaim(claimId);

        Claim claim = findClaim(claimId);
        requireChallengeAuthor(claim.getChallenge(), user);
        requirePending(claim);

        claim.setStatus(ClaimStatus.DECLINED);
        claim.setDecidedAt(LocalDateTime.now());

        publishDecision(claim, false);

        return toDetailsDto(claim);
    }

    /**
     * Declines every pending claim on a listing and notifies each claimant. Called when the owner
     * closes the listing, since approveClaim then rejects it and the claims would hang forever.
     */
    @Transactional
    public void declinePendingClaimsForReport(Long reportId) {
        for (Claim claim : claimRepository.findByReportIdAndStatus(reportId, ClaimStatus.PENDING)) {
            claim.setStatus(ClaimStatus.DECLINED);
            claim.setDecidedAt(LocalDateTime.now());
            publishDecision(claim, false);
        }
    }

    /**
     * The claimant withdraws a claim still awaiting a decision, freeing the slot in the partial
     * unique index so a corrected claim can follow. It does NOT refund the attempt — otherwise
     * submit-and-withdraw would loop past the limit of two.
     */
    @Transactional
    public ClaimDetailsDto withdrawClaim(Long claimId, String userEmail) {
        User user = findUser(userEmail);

        // Same lock as approveClaim: withdrawing and deciding touch the same claim.
        lockReportOfClaim(claimId);

        Claim claim = findClaim(claimId);

        if (!claim.getClaimant().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("claim.notFound");
        }
        requirePending(claim);

        claim.setStatus(ClaimStatus.WITHDRAWN);
        claim.setDecidedAt(LocalDateTime.now());

        return toDetailsDto(claim);
    }

    /**
     * Declines one claim without an authorisation check, for account blocking only — there the
     * decision is not the author's. Goes through the same path so the claimant is still notified.
     */
    @Transactional
    public void declineClaimOnBlock(Claim claim) {
        if (claim.getStatus() != ClaimStatus.PENDING) {
            return;
        }

        claim.setStatus(ClaimStatus.DECLINED);
        claim.setDecidedAt(LocalDateTime.now());
        publishDecision(claim, false);
    }

    private void publishDecision(Claim claim, boolean approved) {
        Report report = claim.getChallenge().getReport();
        eventPublisher.publishEvent(new ClaimDecidedEvent(
                this, claim.getId(), claim.getChallenge().getId(), report.getId(),
                claim.getClaimant().getId(), report.getTitle(), approved));
    }

    private void requireChallengeAuthor(Challenge challenge, User user) {
        if (!challenge.getAuthor().getId().equals(user.getId())) {
            throw new InvalidClaimException("claim.onlyAuthorCanReview");
        }
    }

    private void requirePending(Claim claim) {
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidClaimException("claim.alreadyDecided");
        }
    }

    /**
     * Locks the listing a claim belongs to. The listing id comes from a projection rather than the
     * claim entity, so the lock is taken before anything is cached.
     */
    private Report lockReportOfClaim(Long claimId) {
        Long reportId = claimRepository.findReportIdByClaimId(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("claim.notFound"));

        return reportFacade.lockById(reportId);
    }

    private Claim findClaim(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("claim.notFound"));
    }

    private ClaimSummaryDto toSummaryDto(Claim claim) {
        int totalChoice = 0;
        int correctChoice = 0;
        for (ClaimAnswer answer : claim.getAnswers()) {
            if (answer.getQuestion().getKind() == QuestionKind.CHOICE) {
                totalChoice++;
                if (Boolean.TRUE.equals(answer.getIsCorrect())) {
                    correctChoice++;
                }
            }
        }

        return new ClaimSummaryDto(
                claim.getId(),
                claim.getStatus(),
                buildDisplayName(claim.getClaimant()),
                claim.getSubmittedAt(),
                correctChoice,
                totalChoice,
                claim.getPhotoUrl() != null
        );
    }

    private ClaimDetailsDto toDetailsDto(Claim claim) {
        List<ClaimReviewAnswerDto> answerDtos = claim.getAnswers().stream()
                .map(answer -> new ClaimReviewAnswerDto(
                        answer.getQuestion().getId(),
                        answer.getQuestion().getPrompt(),
                        answer.getQuestion().getKind(),
                        answer.getAnswerText(),
                        answer.getIsCorrect(),
                        answer.getQuestion().getCorrectAnswer()
                ))
                .toList();

        RevealedContactDto claimantContact = claim.getStatus() == ClaimStatus.APPROVED
                ? contactFor(claim.getClaimant(), claim.getChallenge().getReport())
                : null;

        return new ClaimDetailsDto(
                claim.getId(),
                claim.getChallenge().getId(),
                claim.getChallenge().getReport().getId(),
                claim.getStatus(),
                claim.getClaimant().getId(),
                buildDisplayName(claim.getClaimant()),
                claim.getMessage(),
                claim.getPhotoUrl(),
                claim.getSubmittedAt(),
                claim.getDecidedAt(),
                answerDtos,
                claimantContact
        );
    }

    private RevealedContactDto contactFor(User person, Report report) {
        boolean isReportOwner = report.getUser().getId().equals(person.getId());
        if (isReportOwner && (hasText(report.getContactEmail()) || hasText(report.getContactPhone()))) {
            String email = hasText(report.getContactEmail()) ? report.getContactEmail() : person.getEmail();
            return new RevealedContactDto(buildDisplayName(person), email, report.getContactPhone());
        }
        return new RevealedContactDto(buildDisplayName(person), person.getEmail(), person.getPhoneNumber());
    }

    private String buildDisplayName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String combined = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        String trimmed = combined.trim();
        return trimmed.isEmpty() ? user.getUsername() : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ClaimDto toDto(Claim claim) {
        List<ClaimAnswerDto> answerDtos = claim.getAnswers().stream()
                .map(answer -> new ClaimAnswerDto(
                        answer.getQuestion().getId(),
                        answer.getQuestion().getPrompt(),
                        answer.getAnswerText()
                ))
                .toList();

        RevealedContactDto holderContact = claim.getStatus() == ClaimStatus.APPROVED
                ? contactFor(claim.getChallenge().getAuthor(), claim.getChallenge().getReport())
                : null;

        return new ClaimDto(
                claim.getId(),
                claim.getChallenge().getId(),
                claim.getChallenge().getReport().getId(),
                claim.getChallenge().getReport().getTitle(),
                claim.getStatus(),
                claim.getMessage(),
                claim.getPhotoUrl(),
                claim.getSubmittedAt(),
                claim.getDecidedAt(),
                answerDtos,
                holderContact
        );
    }

    private User findUser(String email) {
        return userFacade.requireByEmail(email);
    }

    private Challenge findChallenge(Long challengeId) {
        return challengeRepository.findById(challengeId)
                .filter(challenge -> challenge.getReport().getStatus() != ReportStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("challenge.notFound"));
    }

    private boolean isOwnerOrAuthor(Report report, Challenge challenge, User user) {
        return report.getUser().getId().equals(user.getId())
                || challenge.getAuthor().getId().equals(user.getId());
    }
}
