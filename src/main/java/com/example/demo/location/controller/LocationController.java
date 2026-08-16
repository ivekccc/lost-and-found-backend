package com.example.demo.location.controller;

import com.example.demo.location.dto.AutoCompleteSuggestionDTO;
import com.example.demo.location.dto.ReverseGeocodeDto;
import com.example.demo.location.service.LocationService;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@Tag(name = "Location", description = "Location endpoints")
public class LocationController {
    private final LocationService locationService;

    @GetMapping("/autocomplete")
    @Operation(summary = "Suggest addresses",
            description = "Results are limited to the city the caller is currently browsing, so an "
                    + "address that could not be used for a report is never offered.")
    public ResponseEntity<List<AutoCompleteSuggestionDTO>> autocomplete(
            @RequestParam String query,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                locationService.getAutoCompleteSuggestions(query, userDetails.getUsername()));
    }

    @GetMapping("/reverse")
    @Operation(summary = "Resolve coordinates to an address",
            description = "Turns a device position into the same kind of suggestion autocomplete returns, "
                    + "so \"use my current location\" ends up on the identical path as picking an address by "
                    + "hand. The city is decided from the matched address rather than the raw fix. The "
                    + "suggestion is returned even when it falls outside the city the caller is browsing — "
                    + "matchesActiveCity says so, and the client can offer to switch city.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address resolved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReverseGeocodeDto.class))),
            @ApiResponse(responseCode = "400", description = "Coordinates out of range"),
            @ApiResponse(responseCode = "404", description = "No address at these coordinates"),
            @ApiResponse(responseCode = "409", description = "The place is outside every supported city")
    })
    public ResponseEntity<ReverseGeocodeDto> reverseGeocode(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                locationService.reverseGeocode(latitude, longitude, userDetails.getUsername()));
    }
}
