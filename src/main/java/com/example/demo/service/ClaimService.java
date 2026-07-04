package com.example.demo.service;

import com.example.demo.dto.ClaimAnswerDto;
import com.example.demo.dto.ClaimAnswerRequestDto;
import com.example.demo.dto.ClaimDetailsDto;
import com.example.demo.dto.ClaimDto;
import com.example.demo.dto.ClaimReviewAnswerDto;
import com.example.demo.dto.ClaimSummaryDto;
import com.example.demo.dto.ClaimantChallengeDto;
import com.example.demo.dto.ClaimantChallengeQuestionDto;
import com.example.demo.dto.CreateClaimRequestDto;
import com.example.demo.dto.RevealedContactDto;
import com.example.demo.event.ClaimDecidedEvent;
import com.example.demo.event.ClaimSubmittedEvent;
import com.example.demo.exception.InvalidClaimException;
import com.example.demo.exception.RateLimitExceededException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Challenge;
import com.example.demo.model.ChallengeQuestion;
import com.example.demo.model.Claim;
import com.example.demo.model.ClaimAnswer;
import com.example.demo.model.ClaimStatus;
import com.example.demo.model.QuestionKind;
import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import com.example.demo.model.User;
import com.example.demo.repository.ChallengeRepository;
import com.example.demo.repository.ClaimRepository;
import com.example.demo.repository.UserRepository;
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
    private static final int MAX_ATTEMPTS_PER_CHALLENGE = 2;
    private static final long DAY_IN_SECONDS = 86400;

    private final ClaimRepository claimRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public ClaimantChallengeDto getChallengeForClaimant(Long challengeId, String userEmail) {
        User user = findUser(userEmail);
        Challenge challenge = findChallenge(challengeId);
        Report report = challenge.getReport();

        if (report.getType() == ReportType.LOST && !isOwnerOrAuthor(report, challenge, user)) {
            throw new InvalidClaimException("Only the report owner can answer this challenge");
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
            throw new InvalidClaimException("You cannot claim your own challenge");
        }
        if (report.getStatus() != ReportStatus.ACTIVE) {
            throw new InvalidClaimException("This report is no longer active");
        }
        if (report.getType() == ReportType.LOST && !report.getUser().getId().equals(claimant.getId())) {
            throw new InvalidClaimException("Only the report owner can answer this challenge");
        }
    }

    private void validateLimits(Long challengeId, Long claimantId) {
        if (claimRepository.existsByChallengeIdAndClaimantIdAndStatus(challengeId, claimantId, ClaimStatus.PENDING)) {
            throw new InvalidClaimException("You already have a pending claim for this challenge");
        }
        if (claimRepository.countByChallengeIdAndClaimantId(challengeId, claimantId) >= MAX_ATTEMPTS_PER_CHALLENGE) {
            throw new InvalidClaimException("You have used all attempts for this challenge");
        }
        long dailyCount = claimRepository.countByClaimantIdSince(claimantId, LocalDateTime.now().minusDays(1));
        if (dailyCount >= MAX_CLAIMS_PER_DAY) {
            throw new RateLimitExceededException(
                    "Daily claim limit reached. Try again tomorrow.", DAY_IN_SECONDS);
        }
    }

    private void buildAnswers(Claim claim, Challenge challenge, List<ClaimAnswerRequestDto> answers) {
        Map<Long, ChallengeQuestion> questionsById = new HashMap<>();
        challenge.getQuestions().forEach(question -> questionsById.put(question.getId(), question));

        Map<Long, ClaimAnswerRequestDto> answersByQuestionId = new HashMap<>();
        for (ClaimAnswerRequestDto answer : answers) {
            if (!questionsById.containsKey(answer.getQuestionId())) {
                throw new InvalidClaimException("Question " + answer.getQuestionId() + " does not belong to this challenge");
            }
            if (answersByQuestionId.put(answer.getQuestionId(), answer) != null) {
                throw new InvalidClaimException("Duplicate answer for question " + answer.getQuestionId());
            }
        }
        if (answersByQuestionId.size() != questionsById.size()) {
            throw new InvalidClaimException("All challenge questions must be answered");
        }

        for (ChallengeQuestion question : challenge.getQuestions()) {
            ClaimAnswerRequestDto answerRequest = answersByQuestionId.get(question.getId());

            ClaimAnswer answer = new ClaimAnswer();
            answer.setClaim(claim);
            answer.setQuestion(question);
            answer.setAnswerText(answerRequest.getAnswerText());

            if (question.getKind() == QuestionKind.CHOICE) {
                if (question.getChoices() == null || !question.getChoices().contains(answerRequest.getAnswerText())) {
                    throw new InvalidClaimException("Answer to question " + question.getId() + " must be one of the offered choices");
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
        Claim claim = findClaim(claimId);
        requireChallengeAuthor(claim.getChallenge(), user);
        requirePending(claim);

        Report report = claim.getChallenge().getReport();

        claim.setStatus(ClaimStatus.APPROVED);
        claim.setDecidedAt(LocalDateTime.now());
        report.setStatus(ReportStatus.MATCHED);

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
        Claim claim = findClaim(claimId);
        requireChallengeAuthor(claim.getChallenge(), user);
        requirePending(claim);

        claim.setStatus(ClaimStatus.DECLINED);
        claim.setDecidedAt(LocalDateTime.now());

        publishDecision(claim, false);

        return toDetailsDto(claim);
    }

    private void publishDecision(Claim claim, boolean approved) {
        Report report = claim.getChallenge().getReport();
        eventPublisher.publishEvent(new ClaimDecidedEvent(
                this, claim.getId(), claim.getChallenge().getId(), report.getId(),
                claim.getClaimant().getId(), report.getTitle(), approved));
    }

    private void requireChallengeAuthor(Challenge challenge, User user) {
        if (!challenge.getAuthor().getId().equals(user.getId())) {
            throw new InvalidClaimException("Only the challenge author can review claims");
        }
    }

    private void requirePending(Claim claim) {
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidClaimException("This claim has already been decided");
        }
    }

    private Claim findClaim(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim with id " + claimId + " not found"));
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
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Challenge findChallenge(Long challengeId) {
        return challengeRepository.findById(challengeId)
                .filter(challenge -> challenge.getReport().getStatus() != ReportStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge with id " + challengeId + " not found"));
    }

    private boolean isOwnerOrAuthor(Report report, Challenge challenge, User user) {
        return report.getUser().getId().equals(user.getId())
                || challenge.getAuthor().getId().equals(user.getId());
    }
}
