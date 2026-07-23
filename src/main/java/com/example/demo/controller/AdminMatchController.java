package com.example.demo.controller;

import com.example.demo.dto.AdminMatchListDto;
import com.example.demo.model.ReportMatchStatus;
import com.example.demo.service.AdminMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/matches")
@RequiredArgsConstructor
@Tag(name = "Admin Matches", description = "Admin read-only overview of generated matches")
public class AdminMatchController {

    private final AdminMatchService adminMatchService;

    @GetMapping
    @Operation(summary = "Get matches (paginated)",
            description = "Returns generated matches with score breakdown, optionally filtered by status and minimum score")
    public ResponseEntity<Page<AdminMatchListDto>> getMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ReportMatchStatus status,
            @RequestParam(required = false) Integer minScore) {
        return ResponseEntity.ok(adminMatchService.getMatches(page, size, status, minScore));
    }
}
