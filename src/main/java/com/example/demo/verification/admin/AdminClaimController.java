package com.example.demo.verification.admin;

import com.example.demo.verification.model.ClaimStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin Claims", description = "Admin endpoints for claim oversight")
public class AdminClaimController {

    private final AdminClaimService adminClaimService;

    @GetMapping("/admin/claims")
    @Operation(summary = "Get all claims", description = "Returns all claims across reports, optionally filtered by status")
    public ResponseEntity<List<AdminClaimListDto>> getClaims(@RequestParam(required = false) ClaimStatus status) {
        return ResponseEntity.ok(adminClaimService.getClaims(status));
    }


}
