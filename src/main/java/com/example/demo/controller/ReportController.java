package com.example.demo.controller;

import com.example.demo.dto.CreateReportRequestDto;
import com.example.demo.dto.MatchDto;
import com.example.demo.dto.NearbyReportDTO;
import com.example.demo.dto.ReportDetailsDTO;
import com.example.demo.dto.ReportListDTO;
import com.example.demo.model.ReportType;
import com.example.demo.service.ReportMatchService;
import com.example.demo.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    private final ReportMatchService reportMatchService;

    @PostMapping
    @Operation(summary = "Create report", description = "Creates a new lost or found report")
    @ApiResponse(responseCode = "201", description = "Report created successfully")
    public ResponseEntity<ReportDetailsDTO> createReport(@Valid @RequestBody CreateReportRequestDto createReportRequestDto, @AuthenticationPrincipal UserDetails userDetails) {

        ReportDetailsDTO created = reportService.createReport(createReportRequestDto, userDetails.getUsername());
        return ResponseEntity.ok(created);
    }

    @GetMapping
    @Operation(summary = "Get all reports", description = "Returns active reports from other users (the caller's own reports are excluded)")
    public ResponseEntity<List<ReportListDTO>> getReports(
            @RequestParam(required = false) ReportType type,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ReportListDTO> reports = reportService.getReports(type, search, userDetails.getUsername());
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/mine")
    @Operation(summary = "Get my reports", description = "Returns the current user's own reports (all statuses except deleted)")
    public ResponseEntity<List<ReportListDTO>> getMyReports(@AuthenticationPrincipal UserDetails userDetails) {
        List<ReportListDTO> reports = reportService.getMyReports(userDetails.getUsername());
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get found reports nearby",
            description = "Returns FOUND reports from other users within radiusKm of the given coordinates, sorted by distance ascending, each with its distance in km")
    public ResponseEntity<List<NearbyReportDTO>> getNearbyReports(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5") double radiusKm,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<NearbyReportDTO> reports = reportService.getNearbyReports(
                latitude, longitude, radiusKm, userDetails.getUsername());
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}/matches")
    @Operation(summary = "Get matches for my report",
            description = "Returns suggested matches for the given report, sorted by score descending. Only the report owner can access them.")
    public ResponseEntity<List<MatchDto>> getReportMatches(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<MatchDto> matches = reportMatchService.getMatchesForReport(id, userDetails.getUsername());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get report by ID", description = "Returns detailed information about a specific report. Found-report photos are only included for the report owner.")
    public ResponseEntity<ReportDetailsDTO> getReportById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reportService.getReportById(id, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
