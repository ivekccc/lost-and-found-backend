package com.example.demo.controller;

import com.example.demo.dto.AbuseReportDto;
import com.example.demo.dto.CreateAbuseReportRequestDto;
import com.example.demo.service.AbuseReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/abuse-reports")
@RequiredArgsConstructor
@Tag(name = "Abuse Reports", description = "Report suspicious listings or users")
public class AbuseReportController {

    private final AbuseReportService abuseReportService;

    @PostMapping
    @Operation(summary = "Report a listing or user",
            description = "Flags a suspicious listing or user for admin review. Rate-limited; you cannot report yourself or open a second report for the same target.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Report submitted",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AbuseReportDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid report (self-report, duplicate open report)"),
            @ApiResponse(responseCode = "404", description = "Target not found"),
            @ApiResponse(responseCode = "429", description = "Daily report limit reached")
    })
    public ResponseEntity<AbuseReportDto> report(
            @Valid @RequestBody CreateAbuseReportRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {

        AbuseReportDto created = abuseReportService.createReport(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
