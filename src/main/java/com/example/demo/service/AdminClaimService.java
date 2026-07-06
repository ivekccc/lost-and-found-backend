package com.example.demo.service;

import com.example.demo.dto.AdminClaimListDto;
import com.example.demo.dto.UpdateCategoryImageRequestDto;
import com.example.demo.dto.UpdateMinQuestionsRequestDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Claim;
import com.example.demo.model.ClaimStatus;
import com.example.demo.model.ReportCategory;
import com.example.demo.model.User;
import com.example.demo.repository.ClaimRepository;
import com.example.demo.repository.ReportCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminClaimService {

    private final ClaimRepository claimRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional(readOnly = true)
    public List<AdminClaimListDto> getClaims(ClaimStatus status) {
        List<Claim> claims = status != null
                ? claimRepository.findByStatusOrderBySubmittedAtDesc(status)
                : claimRepository.findAllByOrderBySubmittedAtDesc();

        return claims.stream().map(this::toDto).toList();
    }

    @Transactional
    public void updateMinQuestions(Long categoryId, UpdateMinQuestionsRequestDto request) {
        ReportCategory category = reportCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setMinQuestions(request.getMinQuestions());
        reportCategoryRepository.save(category);
    }

    @Transactional
    public void updateCategoryImage(Long categoryId, UpdateCategoryImageRequestDto request) {
        ReportCategory category = reportCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        String previousPublicId = category.getImagePublicId();

        category.setImageUrl(request.getImageUrl());
        category.setImagePublicId(request.getImagePublicId());
        reportCategoryRepository.save(category);

        if (previousPublicId != null && !previousPublicId.equals(request.getImagePublicId())) {
            cloudinaryService.deleteImage(previousPublicId);
        }
    }

    private AdminClaimListDto toDto(Claim claim) {
        return new AdminClaimListDto(
                claim.getId(),
                claim.getChallenge().getReport().getId(),
                claim.getChallenge().getReport().getTitle(),
                claim.getChallenge().getReport().getType(),
                claim.getChallenge().getId(),
                buildDisplayName(claim.getChallenge().getAuthor()),
                buildDisplayName(claim.getClaimant()),
                claim.getStatus(),
                claim.getSubmittedAt(),
                claim.getDecidedAt()
        );
    }

    private String buildDisplayName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String combined = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        String trimmed = combined.trim();
        return trimmed.isEmpty() ? user.getUsername() : trimmed;
    }
}
