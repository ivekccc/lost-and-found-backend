package com.example.demo.report.admin;

import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    @Operation(summary = "List reports for moderation",
            description = "All statuses except DELETED, any owner, exact location. Paginated and sortable by "
                    + "createdAt, title, status, type or id. The search matches the id (with or without #) or the "
                    + "title and description, ignoring diacritics.")
    @ApiResponse(responseCode = "200", description = "One page of reports")
    public ResponseEntity<Page<AdminReportListDto>> getReports(
            @RequestParam(required = false) ReportType type,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return ResponseEntity.ok(adminReportService.getReports(
                type, status, cityId, ownerId, search, page, size, sort, direction));
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
