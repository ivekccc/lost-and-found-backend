package com.example.demo.verification.admin;

import com.example.demo.verification.model.ClaimStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin Claims", description = "Admin endpoints for claim oversight")
public class AdminClaimController {

    private final AdminClaimService adminClaimService;

    @GetMapping("/admin/claims")
    @Operation(summary = "List ownership claims", description = "All claims across reports, paginated, sortable by submittedAt, status or id, filterable by status, claimant and report.")
    @ApiResponse(responseCode = "200", description = "One page of claims")
    public ResponseEntity<Page<AdminClaimListDto>> getClaims(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) Long claimantId,
            @RequestParam(required = false) Long reportId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return ResponseEntity.ok(adminClaimService.getClaims(status, claimantId, reportId, page, size, sort, direction));
    }
}
