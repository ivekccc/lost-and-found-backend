package com.example.demo.controller;

import com.example.demo.dto.CreateReportRequestDto;
import com.example.demo.dto.ReportContactDTO;
import com.example.demo.dto.ReportDetailsDTO;
import com.example.demo.dto.ReportListDTO;
import com.example.demo.model.ReportType;
import com.example.demo.service.ContactRevealService;
import com.example.demo.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report management endpoints")
public class ReportController {
    private final ReportService reportService;
    private final ContactRevealService contactRevealService;

    @PostMapping
    @Operation(summary = "Create report", description = "Creates a new lost or found report")
    @ApiResponse(responseCode = "201", description = "Report created successfully")
    public ResponseEntity<ReportDetailsDTO> createReport(@Valid @RequestBody CreateReportRequestDto createReportRequestDto, @AuthenticationPrincipal UserDetails userDetails) {

        ReportDetailsDTO created = reportService.createReport(createReportRequestDto, userDetails.getUsername());
        return ResponseEntity.ok(created);
    }

    @GetMapping
    @Operation(summary = "Get all reports", description = "Returns a list of all active reports")
    public ResponseEntity<List<ReportListDTO>> getReports(
            @RequestParam(required = false) ReportType type,
            @RequestParam(required = false) String search) {
        List<ReportListDTO> reports = reportService.getReports(type, search);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get report by ID", description = "Returns detailed information about a specific report")
    public ResponseEntity<ReportDetailsDTO> getReportById(@PathVariable Long id) {
        return reportService.getReportById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/contact")
    @Operation(summary = "Reveal report contact info", description = "Returns the contact email and phone for a report. Rate-limited per user and audit-logged.")
    public ResponseEntity<ReportContactDTO> revealContact(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        ReportContactDTO contact = contactRevealService.revealContact(
                id,
                userDetails.getUsername(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        return ResponseEntity.ok(contact);
    }
}
