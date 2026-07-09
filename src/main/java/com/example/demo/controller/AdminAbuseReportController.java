package com.example.demo.controller;

import com.example.demo.dto.AbuseReportDto;
import com.example.demo.model.AbuseReportStatus;
import com.example.demo.service.AbuseReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/admin/abuse-reports")
@RequiredArgsConstructor
@Tag(name = "Admin Abuse Reports", description = "Admin moderation queue for reported listings and users")
public class AdminAbuseReportController {

    private final AbuseReportService abuseReportService;

    @GetMapping
    @Operation(summary = "Get abuse reports", description = "Returns abuse reports, optionally filtered by status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved abuse reports",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = AbuseReportDto.class))))
    })
    public ResponseEntity<List<AbuseReportDto>> getReports(@RequestParam(required = false) AbuseReportStatus status) {
        return ResponseEntity.ok(abuseReportService.getReports(status));
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
