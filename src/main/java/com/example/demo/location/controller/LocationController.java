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
    @Operation(summary = "Describe a point on the map",
            description = "Used by the map picker under the pin. The city is decided from the point itself, "
                    + "because the point is what a listing stores. The nearest address is returned as a "
                    + "suggestion, or null when the point has none (a park, a field). The answer is given "
                    + "even when the point is outside the city the caller is browsing — matchesActiveCity "
                    + "says so, and the client can offer to switch city.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address resolved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReverseGeocodeDto.class))),
            @ApiResponse(responseCode = "400", description = "Coordinates out of range"),
            @ApiResponse(responseCode = "409", description = "The point is outside every supported city"),
            @ApiResponse(responseCode = "429", description = "The address provider is busy; retry after a moment")
    })
    public ResponseEntity<ReverseGeocodeDto> reverseGeocode(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                locationService.reverseGeocode(latitude, longitude, userDetails.getUsername()));
    }
}
