package com.example.demo.report.admin;

import com.example.demo.report.dto.UpdateCategoryImageRequestDto;
import com.example.demo.report.dto.UpdateMinQuestionsRequestDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole(\'ADMIN\')")
@RestController
@RequestMapping("/admin/report-categories")
@RequiredArgsConstructor
@Tag(name = "Admin Report Categories", description = "Admin endpoints for report category settings")
public class AdminReportCategoryController {

    private final AdminReportCategoryService adminReportCategoryService;

    @PutMapping("/{id}/min-questions")
    @Operation(summary = "Update category minimum questions", description = "Sets the minimum number of verification questions required when composing a challenge in this category")
    public ResponseEntity<Void> updateMinQuestions(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMinQuestionsRequestDto request) {

        adminReportCategoryService.updateMinQuestions(id, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/image")
    @Operation(summary = "Update category image", description = "Sets the category placeholder image shown instead of found-report photos; the previous Cloudinary image is deleted")
    public ResponseEntity<Void> updateCategoryImage(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryImageRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {

        adminReportCategoryService.updateCategoryImage(id, request, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
