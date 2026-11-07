package com.example.demo.matching.admin;

import com.example.demo.matching.model.ReportMatchStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@PreAuthorize("hasRole(\'ADMIN\')")
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

    @GetMapping("/{id}")
    @Operation(summary = "Get match details",
            description = "One match with the inputs, the scoring parameters applied when it was "
                    + "computed, and the resulting points per component")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Match found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminMatchDetailsDto.class))),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public ResponseEntity<AdminMatchDetailsDto> getMatch(@PathVariable Long id) {
        return ResponseEntity.ok(adminMatchService.getMatch(id));
    }
}
