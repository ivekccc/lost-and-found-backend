package com.example.demo.controller;

import com.example.demo.dto.AdminReportDetailsDTO;
import com.example.demo.dto.AdminReportListDto;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import com.example.demo.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports", description = "Admin endpoints for managing reports")
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    @Operation(summary = "Get reports for moderation",
            description = "Returns all reports except deleted ones — every status (including flagged) and every owner, always with the exact location. Optionally narrowed to one city; unlike app users, an admin has no city of their own and sees all of them by default. The public GET /reports must not be used for moderation: it only returns ACTIVE reports, excludes the caller's own, and masks locations to zone level (a local community of about 1 km², or the city municipality where no finer unit exists).")
    @ApiResponse(responseCode = "200", description = "Reports returned",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AdminReportListDto.class))))
    public ResponseEntity<List<AdminReportListDto>> getReports(
            @RequestParam(required = false) ReportType type,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long cityId) {
        return ResponseEntity.ok(adminReportService.getReports(type, status, cityId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get report by id", description = "Returns full report details including contact info")
    public ResponseEntity<AdminReportDetailsDTO> getReportById(@PathVariable Long id) {
        AdminReportDetailsDTO report = adminReportService.getReportById(id);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{id}/flag")
    @Operation(summary = "Flag report", description = "Hides the listing from public lists and resolves open reports against it")
    public ResponseEntity<Void> flagReport(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        adminReportService.flagReport(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unflag")
    @Operation(summary = "Unflag report", description = "Restores a flagged listing to active")
    public ResponseEntity<Void> unflagReport(@PathVariable Long id) {
        adminReportService.unflagReport(id);
        return ResponseEntity.noContent().build();
    }
}
