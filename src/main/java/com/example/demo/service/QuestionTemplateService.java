package com.example.demo.service;

import com.example.demo.dto.QuestionTemplateDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.QuestionTemplateRepository;
import com.example.demo.repository.ReportCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionTemplateService {

    private final QuestionTemplateRepository questionTemplateRepository;
    private final ReportCategoryRepository reportCategoryRepository;

    public List<QuestionTemplateDto> getActiveTemplatesForCategory(Long categoryId) {
        if (!reportCategoryRepository.existsById(categoryId)) {
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
