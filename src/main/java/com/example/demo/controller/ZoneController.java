package com.example.demo.controller;

import com.example.demo.dto.CityZoneDto;
import com.example.demo.service.ZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/zones")
@RequiredArgsConstructor
@Tag(name = "Zones", description = "Parts of a city, used to narrow search below city level")
public class ZoneController {

    private final ZoneService zoneService;

    @GetMapping("/areas")
    @Operation(summary = "Coarse areas of my city",
            description = "The larger administrative units of the city the caller is browsing — "
                    + "municipalities in Belgrade. Cities that have no such subdivision answer with a "
                    + "single entry covering the whole city; a list shorter than two means there is "
                    + "nothing to choose from and the control should not be shown.")
    @ApiResponse(responseCode = "200", description = "Areas of the caller's active city",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = CityZoneDto.class))))
    public ResponseEntity<List<CityZoneDto>> getAreas(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(zoneService.getAreas(userDetails.getUsername()));
    }

    @GetMapping("/neighbourhoods")
    @Operation(summary = "Fine-grained parts of my city (paginated)",
            description = "Local communities and settlements of the city the caller is browsing. "
                    + "Narrowed to one area when areaId is given, and searched by name when search is "
                    + "given — the search ignores diacritics, so \"cukarica\" finds \"Čukarica\". "
                    + "Every entry carries the area it belongs to, so picking one is enough to fill in "
                    + "both levels of the filter.")
    @ApiResponse(responseCode = "200", description = "One page of parts, ordered by name")
    public ResponseEntity<Page<CityZoneDto>> getNeighbourhoods(
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(zoneService.getNeighbourhoods(
                userDetails.getUsername(), areaId, search, page, size));
    }
}
