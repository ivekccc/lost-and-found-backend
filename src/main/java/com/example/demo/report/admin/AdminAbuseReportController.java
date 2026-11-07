package com.example.demo.report.admin;

import com.example.demo.report.dto.AbuseReportDto;
import com.example.demo.report.model.AbuseReason;
import com.example.demo.report.model.AbuseReportStatus;
import com.example.demo.report.model.AbuseTargetType;
import com.example.demo.report.model.Report;
import com.example.demo.report.service.AbuseReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@PreAuthorize("hasRole(\'ADMIN\')")
@RestController
@RequestMapping("/admin/abuse-reports")
@RequiredArgsConstructor
@Tag(name = "Admin Abuse Reports", description = "Admin moderation queue for reported listings and users")
public class AdminAbuseReportController {

    private final AbuseReportService abuseReportService;

    @GetMapping
    @Operation(summary = "List abuse reports",
            description = "Paginated and sortable by createdAt, status, reason or id; filterable by status, target type, "
                    + "reason, the reported listing or the reported user.")
    @ApiResponse(responseCode = "200", description = "One page of abuse reports")
    public ResponseEntity<Page<AbuseReportDto>> getReports(
            @RequestParam(required = false) AbuseReportStatus status,
            @RequestParam(required = false) AbuseTargetType targetType,
            @RequestParam(required = false) AbuseReason reason,
            @RequestParam(required = false) Long reportId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return ResponseEntity.ok(abuseReportService.getReports(
                status, targetType, reason, reportId, userId, page, size, sort, direction));
    }

    @PostMapping("/{id}/dismiss")
    @Operation(summary = "Dismiss abuse report", description = "Marks a report as dismissed without action")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Report dismissed"),
            @ApiResponse(responseCode = "404", description = "Abuse report not found")
    })
    public ResponseEntity<Void> dismiss(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        abuseReportService.dismiss(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
