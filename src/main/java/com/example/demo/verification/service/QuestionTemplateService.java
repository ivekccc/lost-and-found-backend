package com.example.demo.verification.service;

import org.springframework.context.i18n.LocaleContextHolder;

import com.example.demo.report.ReportFacade;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.verification.dto.QuestionTemplateDto;
import com.example.demo.verification.repository.QuestionTemplateRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
@RequiredArgsConstructor
public class QuestionTemplateService {

    private final QuestionTemplateRepository questionTemplateRepository;
    private final ReportFacade reportFacade;

    public List<QuestionTemplateDto> getActiveTemplatesForCategory(Long categoryId) {
        if (!reportFacade.categoryExists(categoryId)) {
            throw new ResourceNotFoundException("category.notFound");
        }

        return questionTemplateRepository.findByCategoryIdAndIsActiveTrueOrderByIdAsc(categoryId).stream()
                .map(template -> new QuestionTemplateDto(
                        template.getId(),
                        template.localizedPrompt(LocaleContextHolder.getLocale()),
                        template.getKind(),
                        template.localizedDefaultChoices(LocaleContextHolder.getLocale())
                ))
                .toList();
    }
}
