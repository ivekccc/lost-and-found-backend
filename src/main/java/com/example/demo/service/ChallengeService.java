package com.example.demo.service;

import com.example.demo.dto.ChallengeDto;
import com.example.demo.dto.ChallengeQuestionDto;
import com.example.demo.dto.ChallengeQuestionRequestDto;
import com.example.demo.dto.CreateChallengeRequestDto;
import com.example.demo.dto.ReportChallengeDto;
import com.example.demo.event.ChallengeCreatedEvent;
import com.example.demo.exception.InvalidChallengeException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Challenge;
import com.example.demo.model.ChallengeQuestion;
import com.example.demo.model.Claim;
import com.example.demo.model.QuestionKind;
import com.example.demo.model.QuestionSource;
import com.example.demo.model.QuestionTemplate;
import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import com.example.demo.model.User;
import com.example.demo.repository.ChallengeRepository;
import com.example.demo.repository.ClaimRepository;
import com.example.demo.repository.QuestionTemplateRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.UserRepository;
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
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<ReportChallengeDto> getChallengesForReportOwner(Long reportId, String userEmail) {
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Report report = reportRepository.findById(reportId)
                .filter(r -> r.getStatus() != ReportStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Report with id " + reportId + " not found"));

        if (report.getType() != ReportType.LOST) {
            throw new InvalidChallengeException("Only lost reports receive challenges from finders");
        }
        if (!report.getUser().getId().equals(owner.getId())) {
            throw new InvalidChallengeException("Only the report owner can view received challenges");
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
        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Report report = reportRepository.findById(reportId)
                .filter(r -> r.getStatus() != ReportStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Report with id " + reportId + " not found"));

        if (report.getType() != ReportType.LOST) {
            throw new InvalidChallengeException(
                    "Challenges can only be created on lost reports — found reports get theirs at creation");
        }
        if (report.getStatus() != ReportStatus.ACTIVE) {
            throw new InvalidChallengeException("This report is no longer active");
        }
        if (report.getUser().getId().equals(author.getId())) {
            throw new InvalidChallengeException("You cannot create a challenge on your own report");
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
                    "At least " + minQuestions + " verification questions are required for this category");
        }

        if (challengeRepository.findByReportIdAndAuthorId(report.getId(), author.getId()).isPresent()) {
            throw new InvalidChallengeException("You already created a challenge for this report");
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
                throw new InvalidChallengeException("Choice questions require at least 2 choices");
            }
            if (dto.getCorrectAnswer() == null || dto.getCorrectAnswer().isBlank()) {
                throw new InvalidChallengeException("Choice questions require a correct answer");
            }
            if (!dto.getChoices().contains(dto.getCorrectAnswer())) {
                throw new InvalidChallengeException("Correct answer must be one of the choices");
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
                throw new InvalidChallengeException("Template questions require a template id");
            }
            QuestionTemplate template = questionTemplateRepository.findById(dto.getTemplateId())
                    .orElseThrow(() -> new InvalidChallengeException("Question template not found"));
            if (!template.getCategory().getId().equals(categoryId)) {
                throw new InvalidChallengeException("Question template does not belong to the report category");
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
