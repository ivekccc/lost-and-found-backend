package com.example.demo.controller;

import com.example.demo.dto.ClaimDetailsDto;
import com.example.demo.dto.ClaimDto;
import com.example.demo.dto.ClaimSummaryDto;
import com.example.demo.dto.CreateClaimRequestDto;
import com.example.demo.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Claims", description = "Ownership claim endpoints")
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping("/challenges/{challengeId}/claims")
    @Operation(summary = "Submit ownership claim",
            description = "The claimant answers all challenge questions, optionally attaching a message and a photo. Choice answers are auto-graded.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Claim submitted",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClaimDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid claim (own challenge, missing answers, attempts used up)"),
            @ApiResponse(responseCode = "404", description = "Challenge not found"),
            @ApiResponse(responseCode = "429", description = "Daily claim limit reached")
    })
    public ResponseEntity<ClaimDto> submitClaim(
            @PathVariable Long challengeId,
            @Valid @RequestBody CreateClaimRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ClaimDto created = claimService.submitClaim(challengeId, userDetails.getUsername(), request);
        URI location = URI.create("/claims/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/challenges/{challengeId}/claims")
    @Operation(summary = "List claims on a challenge",
            description = "Challenge author only — returns submitted claims with auto-grade summary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved claims",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ClaimSummaryDto.class)))),
            @ApiResponse(responseCode = "400", description = "Not the challenge author"),
            @ApiResponse(responseCode = "404", description = "Challenge not found")
    })
    public ResponseEntity<List<ClaimSummaryDto>> getClaimsForChallenge(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(claimService.getClaimsForChallenge(challengeId, userDetails.getUsername()));
    }

    @GetMapping("/claims/{id}")
    @Operation(summary = "Get claim details for review",
            description = "Challenge author only — answers with auto-grades and the author's expected answers; claimant contact included once approved")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved claim",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClaimDetailsDto.class))),
            @ApiResponse(responseCode = "400", description = "Not the challenge author"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    public ResponseEntity<ClaimDetailsDto> getClaimDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(claimService.getClaimDetails(id, userDetails.getUsername()));
    }

    @PostMapping("/claims/{id}/approve")
    @Operation(summary = "Approve claim",
            description = "Confirms ownership: reveals contact both ways, marks the report as MATCHED and auto-declines other pending claims")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Claim approved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClaimDetailsDto.class))),
            @ApiResponse(responseCode = "400", description = "Not the challenge author or claim already decided"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    public ResponseEntity<ClaimDetailsDto> approveClaim(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(claimService.approveClaim(id, userDetails.getUsername()));
    }

    @PostMapping("/claims/{id}/decline")
    @Operation(summary = "Decline claim",
            description = "Declines the claim; the claimant is notified and may retry once if attempts remain")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Claim declined",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClaimDetailsDto.class))),
            @ApiResponse(responseCode = "400", description = "Not the challenge author or claim already decided"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    public ResponseEntity<ClaimDetailsDto> declineClaim(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(claimService.declineClaim(id, userDetails.getUsername()));
    }

    @GetMapping("/users/me/claims")
    @Operation(summary = "Get my claims",
            description = "Claims submitted by the current user; holder contact included once approved")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved claims",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ClaimDto.class))))
    })
    public ResponseEntity<List<ClaimDto>> getMyClaims(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(claimService.getMyClaims(userDetails.getUsername()));
    }
}
