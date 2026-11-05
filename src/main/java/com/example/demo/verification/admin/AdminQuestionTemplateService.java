package com.example.demo.verification.admin;

import org.springframework.context.i18n.LocaleContextHolder;

import com.example.demo.report.model.ReportCategory;
import com.example.demo.report.ReportFacade;
import com.example.demo.shared.exception.InvalidChallengeException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.verification.dto.CreateQuestionTemplateRequestDto;
import com.example.demo.verification.dto.UpdateQuestionTemplateRequestDto;
import com.example.demo.verification.model.QuestionKind;
import com.example.demo.verification.model.QuestionTemplate;
import com.example.demo.verification.repository.QuestionTemplateRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@RequiredArgsConstructor
public class AdminQuestionTemplateService {

    private final QuestionTemplateRepository questionTemplateRepository;
    private final ReportFacade reportFacade;

    @Transactional(readOnly = true)
    public List<AdminQuestionTemplateDto> getTemplates(Long categoryId) {
        List<QuestionTemplate> templates = categoryId != null
                ? questionTemplateRepository.findByCategoryIdOrderByIdAsc(categoryId)
                : questionTemplateRepository.findAll();

        return templates.stream().map(this::toDto).toList();
    }

    @Transactional
    public AdminQuestionTemplateDto createTemplate(CreateQuestionTemplateRequestDto request) {
        ReportCategory category = reportFacade.requireCategoryById(request.getCategoryId());

        validateChoices(request.getKind(), request.getDefaultChoices());
        validateChoices(request.getKind(), request.getDefaultChoicesSr());

        QuestionTemplate template = new QuestionTemplate();
        template.setCategory(category);
        template.setPrompt(request.getPrompt());
        template.setPromptSr(request.getPromptSr());
        template.setKind(request.getKind());
        template.setDefaultChoices(request.getDefaultChoices());
        template.setDefaultChoicesSr(request.getDefaultChoicesSr());

        return toDto(questionTemplateRepository.save(template));
    }

    @Transactional
    public AdminQuestionTemplateDto updateTemplate(Long id, UpdateQuestionTemplateRequestDto request) {
        QuestionTemplate template = findTemplate(id);

        validateChoices(request.getKind(), request.getDefaultChoices());
        validateChoices(request.getKind(), request.getDefaultChoicesSr());

        template.setPrompt(request.getPrompt());
        template.setPromptSr(request.getPromptSr());
        template.setKind(request.getKind());
        template.setDefaultChoices(request.getDefaultChoices());
        template.setDefaultChoicesSr(request.getDefaultChoicesSr());
        template.setIsActive(request.getIsActive());

        return toDto(questionTemplateRepository.save(template));
    }

    @Transactional
    public void deactivateTemplate(Long id) {
        QuestionTemplate template = findTemplate(id);
        template.setIsActive(false);
        questionTemplateRepository.save(template);
    }

    private void validateChoices(QuestionKind kind, List<String> choices) {
        if (kind == QuestionKind.CHOICE && (choices == null || choices.size() < 2)) {
            throw new InvalidChallengeException("template.choiceNeedsTwoChoices");
        }
    }

    private QuestionTemplate findTemplate(Long id) {
        return questionTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("template.notFound"));
    }

    private AdminQuestionTemplateDto toDto(QuestionTemplate template) {
        return new AdminQuestionTemplateDto(
                template.getId(),
                template.getCategory().getId(),
                template.getCategory().localizedName(LocaleContextHolder.getLocale()),
                template.getPrompt(),
                template.getPromptSr(),
                template.getKind(),
                template.getDefaultChoices(),
                template.getDefaultChoicesSr(),
                template.getIsActive(),
                template.getCreatedAt()
        );
    }
}
