package com.example.demo.report.admin;

import com.example.demo.audit.AdminActionEvent;
import com.example.demo.audit.AuditAction;
import com.example.demo.audit.AuditTargetType;
import com.example.demo.report.dto.UpdateCategoryImageRequestDto;
import com.example.demo.report.dto.UpdateMinQuestionsRequestDto;
import com.example.demo.report.model.ReportCategory;
import com.example.demo.report.repository.ReportCategoryRepository;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.shared.media.CloudinaryService;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminReportCategoryService {

    private final ReportCategoryRepository reportCategoryRepository;
    private final CloudinaryService cloudinaryService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void updateMinQuestions(Long categoryId, UpdateMinQuestionsRequestDto request) {
        ReportCategory category = requireCategory(categoryId);
        int previous = category.getMinQuestions();
        category.setMinQuestions(request.getMinQuestions());
        reportCategoryRepository.save(category);
        eventPublisher.publishEvent(new AdminActionEvent(AuditAction.CATEGORY_MIN_QUESTIONS_CHANGED, AuditTargetType.CATEGORY,
                categoryId, "Changed minimum questions for " + category.getName(),
                Map.of("from", previous, "to", request.getMinQuestions())));
    }

    @Transactional
    public void updateCategoryImage(Long categoryId, UpdateCategoryImageRequestDto request) {
        ReportCategory category = requireCategory(categoryId);
        String previousPublicId = category.getImagePublicId();

        category.setImageUrl(request.getImageUrl());
        category.setImagePublicId(request.getImagePublicId());
        reportCategoryRepository.save(category);
        eventPublisher.publishEvent(AdminActionEvent.of(AuditAction.CATEGORY_IMAGE_CHANGED, AuditTargetType.CATEGORY,
                categoryId, "Changed the image of " + category.getName()));

        // Only after the new image is stored, and only when it actually replaced something —
        // deleting first would leave the category without a picture if the save failed.
        if (previousPublicId != null && !previousPublicId.equals(request.getImagePublicId())) {
            cloudinaryService.deleteImage(previousPublicId);
        }
    }

    private ReportCategory requireCategory(Long categoryId) {
        return reportCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("category.notFound"));
    }
}
