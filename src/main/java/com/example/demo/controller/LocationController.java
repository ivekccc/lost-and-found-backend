package com.example.demo.controller;


import com.example.demo.dto.AutoCompleteSuggestionDTO;
import com.example.demo.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
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
}
