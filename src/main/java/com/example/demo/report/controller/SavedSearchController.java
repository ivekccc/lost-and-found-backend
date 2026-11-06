package com.example.demo.report.controller;

import com.example.demo.report.dto.CreateSavedSearchRequestDto;
import com.example.demo.report.dto.SavedSearchDto;
import com.example.demo.report.service.SavedSearchService;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/saved-searches")
@RequiredArgsConstructor
@Tag(name = "Saved searches", description = "Searches the user is notified about")
public class SavedSearchController {

    private final SavedSearchService savedSearchService;

    @GetMapping
    @Operation(summary = "Get saved searches",
            description = "The caller's saved searches, newest first, across all cities they were saved in.")
    @ApiResponse(responseCode = "200", description = "Saved searches returned",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = SavedSearchDto.class))))
    public ResponseEntity<List<SavedSearchDto>> getSavedSearches(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(savedSearchService.getSavedSearches(userDetails.getUsername()));
    }

    @PostMapping
    @Operation(summary = "Save a search",
            description = "Stores the given filters for the caller's active city. Every new report in "
                    + "that city matching all set filters notifies the caller — in the inbox always, "
                    + "and as a push up to a daily limit. At least one filter is required and a user "
                    + "can keep at most 10 searches.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Search saved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SavedSearchDto.class))),
            @ApiResponse(responseCode = "400", description = "No filter set, limit reached, or zone outside the active city"),
            @ApiResponse(responseCode = "404", description = "No such category"),
            @ApiResponse(responseCode = "409", description = "The same search is already saved")
    })
    public ResponseEntity<SavedSearchDto> createSavedSearch(
            @Valid @RequestBody CreateSavedSearchRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SavedSearchDto created = savedSearchService.createSavedSearch(request, userDetails.getUsername());
        return ResponseEntity.created(URI.create("/saved-searches/" + created.getId())).body(created);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a saved search")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "No such saved search of the caller")
    })
    public ResponseEntity<Void> deleteSavedSearch(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        savedSearchService.deleteSavedSearch(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
