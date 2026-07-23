package com.example.demo.controller;

import com.example.demo.dto.MatchDto;
import com.example.demo.service.ReportMatchService;
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
@RequestMapping("/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Suggested lost-found match endpoints")
public class MatchController {

    private final ReportMatchService reportMatchService;

    @GetMapping("/mine")
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

    @PostMapping("/{id}/dismiss")
    @Operation(summary = "Dismiss a match",
            description = "Dismisses the match for the calling side only — the other participant still sees it. Idempotent.")
    @ApiResponse(responseCode = "204", description = "Match dismissed")
    public ResponseEntity<Void> dismissMatch(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        reportMatchService.dismissMatch(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
