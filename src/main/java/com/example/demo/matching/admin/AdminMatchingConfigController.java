package com.example.demo.matching.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole(\'ADMIN\')")
@RestController
@RequestMapping("/admin/matching-config")
@RequiredArgsConstructor
@Tag(name = "Admin Matching Config", description = "Scoring parameters of the matching engine")
public class AdminMatchingConfigController {

    private final AdminMatchingConfigService adminMatchingConfigService;

    @GetMapping
    @Operation(summary = "Get matching parameters",
            description = "The values the engine is scoring with right now")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current parameters",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MatchingConfigDto.class)))
    })
    public ResponseEntity<MatchingConfigDto> getMatchingConfig() {
        return ResponseEntity.ok(adminMatchingConfigService.get());
    }

    @PutMapping
    @Operation(summary = "Update matching parameters",
            description = "Applies to matches computed from now on. Matches already stored keep the "
                    + "parameters they were scored with, so their breakdown stays reproducible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parameters updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MatchingConfigDto.class))),
            @ApiResponse(responseCode = "400", description = "A value is out of range, or the three weights do not add up to 100")
    })
    public ResponseEntity<MatchingConfigDto> updateMatchingConfig(
            @Valid @RequestBody UpdateMatchingConfigRequestDto request) {

        return ResponseEntity.ok(adminMatchingConfigService.update(request));
    }
}
