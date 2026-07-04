package com.example.demo.controller;

import com.example.demo.dto.AdminClaimListDto;
import com.example.demo.dto.UpdateMinQuestionsRequestDto;
import com.example.demo.model.ClaimStatus;
import com.example.demo.service.AdminClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin Claims", description = "Admin endpoints for claim oversight and category verification settings")
public class AdminClaimController {

    private final AdminClaimService adminClaimService;

    @GetMapping("/admin/claims")
    @Operation(summary = "Get all claims", description = "Returns all claims across reports, optionally filtered by status")
    public ResponseEntity<List<AdminClaimListDto>> getClaims(@RequestParam(required = false) ClaimStatus status) {
        return ResponseEntity.ok(adminClaimService.getClaims(status));
    }

    @PutMapping("/admin/report-categories/{id}/min-questions")
    @Operation(summary = "Update category minimum questions", description = "Sets the minimum number of verification questions required when composing a challenge in this category")
    public ResponseEntity<Void> updateMinQuestions(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMinQuestionsRequestDto request) {

        adminClaimService.updateMinQuestions(id, request);
        return ResponseEntity.noContent().build();
    }
}
