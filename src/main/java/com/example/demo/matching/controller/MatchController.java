package com.example.demo.matching.controller;

import com.example.demo.matching.dto.MatchDto;
import com.example.demo.matching.service.ReportMatchService;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Suggested lost-found match endpoints")
public class MatchController {

    private final ReportMatchService reportMatchService;

    @GetMapping("/matches/mine")
    @Operation(summary = "Get my top matches",
            description = "Returns the current user's highest-scored suggested matches across all their active reports")
    @ApiResponse(responseCode = "200", description = "Matches returned",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = MatchDto.class))))
    public ResponseEntity<List<MatchDto>> getMyMatches(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<MatchDto> matches = reportMatchService.getMyMatches(userDetails.getUsername(), limit);
        return ResponseEntity.ok(matches);
    }

    @PostMapping("/matches/{id}/dismiss")
    @Operation(summary = "Dismiss a match",
            description = "Dismisses the match for the calling side only — the other participant still sees it. Idempotent.")
    @ApiResponse(responseCode = "204", description = "Match dismissed")
    public ResponseEntity<Void> dismissMatch(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        reportMatchService.dismissMatch(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // Path-wise this hangs off a listing, but the question it answers belongs to matching — so the
    // route lives here rather than making the report module reach across.
    @GetMapping("/reports/{id}/matches")
    @Operation(summary = "Get matches for my report",
            description = "Returns suggested matches for the given report, sorted by score descending. Only the report owner can access them.")
    public ResponseEntity<List<MatchDto>> getReportMatches(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<MatchDto> matches = reportMatchService.getMatchesForReport(id, userDetails.getUsername());
        return ResponseEntity.ok(matches);
    }
}
