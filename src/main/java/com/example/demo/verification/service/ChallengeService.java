package com.example.demo.verification.service;

import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;
import com.example.demo.report.ReportFacade;
import com.example.demo.shared.exception.AccountRestrictedException;
import com.example.demo.shared.exception.InvalidChallengeException;
import com.example.demo.user.model.User;
import com.example.demo.user.model.UserStatus;
import com.example.demo.user.UserFacade;
import com.example.demo.verification.dto.ChallengeDto;
import com.example.demo.verification.dto.ChallengeQuestionDto;
import com.example.demo.verification.dto.ChallengeQuestionRequestDto;
import com.example.demo.verification.dto.CreateChallengeRequestDto;
import com.example.demo.verification.dto.ReportChallengeDto;
import com.example.demo.verification.event.ChallengeCreatedEvent;
import com.example.demo.verification.model.Challenge;
import com.example.demo.verification.model.ChallengeQuestion;
import com.example.demo.verification.model.Claim;
import com.example.demo.verification.model.QuestionKind;
import com.example.demo.verification.model.QuestionSource;
import com.example.demo.verification.model.QuestionTemplate;
import com.example.demo.verification.repository.ChallengeRepository;
import com.example.demo.verification.repository.ClaimRepository;
import com.example.demo.verification.repository.QuestionTemplateRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ClaimRepository claimRepository;
    private final QuestionTemplateRepository questionTemplateRepository;
    private final ReportFacade reportFacade;
    private final UserFacade userFacade;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<ReportChallengeDto> getChallengesForReportOwner(Long reportId, String userEmail) {
        User owner = userFacade.requireByEmail(userEmail);
        Report report = reportFacade.requireVisibleById(reportId);

        if (report.getType() != ReportType.LOST) {
            throw new InvalidChallengeException("challenge.onlyLostReceive");
        }
        if (!report.getUser().getId().equals(owner.getId())) {
            throw new InvalidChallengeException("challenge.onlyOwnerCanView");
        }

        return challengeRepository.findByReportIdOrderByCreatedAtAsc(reportId).stream()
                .map(challenge -> toReportChallengeDto(challenge, owner))
                .toList();
    }

    private ReportChallengeDto toReportChallengeDto(Challenge challenge, User owner) {
        List<Claim> myClaims = claimRepository
                .findByChallengeIdAndClaimantIdOrderBySubmittedAtDesc(challenge.getId(), owner.getId());
        Claim latest = myClaims.isEmpty() ? null : myClaims.get(0);

        return new ReportChallengeDto(
                challenge.getId(),
                buildDisplayName(challenge.getAuthor()),
                challenge.getCreatedAt(),
                challenge.getQuestions().size(),
                latest == null ? null : latest.getId(),
                latest == null ? null : latest.getStatus(),
                myClaims.size()
        );
    }

    private String buildDisplayName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String combined = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        String trimmed = combined.trim();
        return trimmed.isEmpty() ? user.getUsername() : trimmed;
    }

    @Transactional
    public ChallengeDto createChallengeForLostReport(Long reportId, String userEmail, CreateChallengeRequestDto request) {
        User author = userFacade.requireByEmail(userEmail);
        Report report = reportFacade.requireVisibleById(reportId);

        if (author.getStatus() == UserStatus.PARTIALLY_BLOCKED) {
            throw new AccountRestrictedException(
                    "challenge.restrictedCannotSend");
        }
        if (report.getType() != ReportType.LOST) {
            throw new InvalidChallengeException(
                    "challenge.onlyOnLost");
        }
        if (report.getStatus() != ReportStatus.ACTIVE) {
            throw new InvalidChallengeException("report.noLongerActive");
        }
        if (report.getUser().getId().equals(author.getId())) {
            throw new InvalidChallengeException("challenge.cannotOnOwn");
        }

        Challenge challenge = createChallenge(report, author, request.getQuestions());

        eventPublisher.publishEvent(new ChallengeCreatedEvent(
                this, challenge.getId(), report.getId(), report.getUser().getId(), report.getTitle()));

        return toDto(challenge);
    }

    @Transactional
    public Challenge createChallenge(Report report, User author, List<ChallengeQuestionRequestDto> questions) {
        int minQuestions = report.getCategory().getMinQuestions();
        if (questions == null || questions.size() < minQuestions) {
            throw new InvalidChallengeException(
                    "challenge.minQuestions", minQuestions);
        }

        if (challengeRepository.findByReportIdAndAuthorId(report.getId(), author.getId()).isPresent()) {
            throw new InvalidChallengeException("challenge.alreadyCreated");
        }

        Challenge challenge = new Challenge();
        challenge.setReport(report);
        challenge.setAuthor(author);

        for (int i = 0; i < questions.size(); i++) {
            challenge.getQuestions().add(buildQuestion(challenge, questions.get(i), i, report.getCategory().getId()));
        }

        return challengeRepository.save(challenge);
    }

    private ChallengeQuestion buildQuestion(Challenge challenge, ChallengeQuestionRequestDto dto, int orderIndex, Long categoryId) {
        if (dto.getKind() == QuestionKind.CHOICE) {
            if (dto.getChoices() == null || dto.getChoices().size() < 2) {
                throw new InvalidChallengeException("challenge.choiceNeedsTwoChoices");
            }
            if (dto.getCorrectAnswer() == null || dto.getCorrectAnswer().isBlank()) {
                throw new InvalidChallengeException("challenge.choiceNeedsCorrectAnswer");
            }
            if (!dto.getChoices().contains(dto.getCorrectAnswer())) {
                throw new InvalidChallengeException("challenge.correctAnswerNotAChoice");
            }
        }

        ChallengeQuestion question = new ChallengeQuestion();
        question.setChallenge(challenge);
        question.setPrompt(dto.getPrompt());
        question.setKind(dto.getKind());
        question.setSource(dto.getSource());
        question.setChoices(dto.getChoices());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setOrderIndex(orderIndex);

        if (dto.getSource() == QuestionSource.TEMPLATE) {
            if (dto.getTemplateId() == null) {
                throw new InvalidChallengeException("challenge.templateIdRequired");
            }
            QuestionTemplate template = questionTemplateRepository.findById(dto.getTemplateId())
                    .orElseThrow(() -> new InvalidChallengeException("template.notFound"));
            if (!template.getCategory().getId().equals(categoryId)) {
                throw new InvalidChallengeException("challenge.templateWrongCategory");
            }
            question.setTemplate(template);
        }

        return question;
    }

    private ChallengeDto toDto(Challenge challenge) {
        List<ChallengeQuestionDto> questionDtos = challenge.getQuestions().stream()
                .map(question -> new ChallengeQuestionDto(
                        question.getId(),
                        question.getPrompt(),
                        question.getKind(),
                        question.getSource(),
                        question.getTemplate() == null ? null : question.getTemplate().getId(),
                        question.getChoices(),
                        question.getCorrectAnswer(),
                        question.getOrderIndex()
                ))
                .toList();

        return new ChallengeDto(
                challenge.getId(),
                challenge.getReport().getId(),
                challenge.getAuthor().getId(),
                challenge.getCreatedAt(),
                questionDtos
        );
    }
}
