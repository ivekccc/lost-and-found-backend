package com.example.demo.controller;

import com.example.demo.dto.CityDto;
import com.example.demo.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
@Tag(name = "Cities", description = "Cities the app has zone coverage for")
public class CityController {

    private final CityService cityService;

    @GetMapping
    @Operation(summary = "List selectable cities",
            description = "Cities with zone coverage ready for use. The user's current choice is on "
                    + "their profile; change it with PUT /users/me/city.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Available cities",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CityDto.class))))
    })
    public ResponseEntity<List<CityDto>> listCities() {
        return ResponseEntity.ok(cityService.listActiveCities());
    }

    @GetMapping("/detect")
    @Operation(summary = "Which city a point falls in",
            description = "Used to offer switching cities when the device location is outside the "
                    + "one the user picked. Returns no content when the point is not covered by any "
                    + "city, which is a normal answer rather than an error.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Point falls inside this city",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CityDto.class))),
            @ApiResponse(responseCode = "204", description = "Point is not covered by any city"),
            @ApiResponse(responseCode = "400", description = "Coordinates out of range")
    })
    public ResponseEntity<CityDto> detectCity(@RequestParam double latitude,
                                              @RequestParam double longitude) {
        return cityService.detectCity(latitude, longitude)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
