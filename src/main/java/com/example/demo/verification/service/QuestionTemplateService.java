package com.example.demo.verification.service;

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
            throw new ResourceNotFoundException("Category with id " + categoryId + " not found");
        }

        return questionTemplateRepository.findByCategoryIdAndIsActiveTrueOrderByIdAsc(categoryId).stream()
                .map(template -> new QuestionTemplateDto(
                        template.getId(),
                        template.getPrompt(),
                        template.getKind(),
                        template.getDefaultChoices()
                ))
                .toList();
    }
}
