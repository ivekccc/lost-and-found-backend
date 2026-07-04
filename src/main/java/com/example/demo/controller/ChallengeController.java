package com.example.demo.controller;

import com.example.demo.dto.ChallengeDto;
import com.example.demo.dto.ClaimantChallengeDto;
import com.example.demo.dto.CreateChallengeRequestDto;
import com.example.demo.service.ChallengeService;
import com.example.demo.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Tag(name = "Challenges", description = "Verification challenge endpoints")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final ClaimService claimService;

    @PostMapping("/reports/{reportId}/challenges")
    @Operation(summary = "Create challenge on a lost report",
            description = "\"I think I found this\" — the finder composes verification questions for the owner of a lost report to answer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Challenge created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChallengeDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid challenge (wrong report type, own report, too few questions, duplicate)"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public ResponseEntity<ChallengeDto> createChallenge(
            @PathVariable Long reportId,
            @Valid @RequestBody CreateChallengeRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ChallengeDto created = challengeService.createChallengeForLostReport(reportId, userDetails.getUsername(), request);
        URI location = URI.create("/challenges/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/challenges/{id}")
    @Operation(summary = "Get challenge for answering",
            description = "Returns the challenge questions for the claimant — correct answers are never included")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved challenge",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClaimantChallengeDto.class))),
            @ApiResponse(responseCode = "400", description = "Not allowed to answer this challenge"),
            @ApiResponse(responseCode = "404", description = "Challenge not found")
    })
    public ResponseEntity<ClaimantChallengeDto> getChallenge(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(claimService.getChallengeForClaimant(id, userDetails.getUsername()));
    }
}
