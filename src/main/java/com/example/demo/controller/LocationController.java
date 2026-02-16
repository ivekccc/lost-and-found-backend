package com.example.demo.controller;


import com.example.demo.dto.AutoCompleteSuggestionDTO;
import com.example.demo.service.LocationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<AutoCompleteSuggestionDTO>> autocomplete(@RequestParam String query){
        return ResponseEntity.ok(locationService.getAutoCompleteSuggestions(query));
    }
}
