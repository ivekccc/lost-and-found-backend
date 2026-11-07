package com.example.demo.dashboard.controller;

import com.example.demo.dashboard.dto.ActivityDayDto;
import com.example.demo.dashboard.dto.CityOverviewDto;
import com.example.demo.dashboard.dto.QueueCountsDto;
import com.example.demo.dashboard.service.AdminDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin dashboard", description = "Moderation queues and platform activity")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/queues")
    @Operation(summary = "Moderation queues", description = "Counts of work waiting for a moderator and the age of the oldest item in each queue.")
    @ApiResponse(responseCode = "200", description = "Queue counts",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = QueueCountsDto.class)))
    public ResponseEntity<QueueCountsDto> getQueues() {
        return ResponseEntity.ok(adminDashboardService.getQueues());
    }

    @GetMapping("/activity")
    @Operation(summary = "Daily activity", description = "Reports posted, reports reunited and users registered per day, oldest day first. "
            + "Days without activity are included with zeros. The span is clamped to 7–90 days.")
    @ApiResponse(responseCode = "200", description = "One entry per day",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ActivityDayDto.class))))
    public ResponseEntity<List<ActivityDayDto>> getActivity(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminDashboardService.getActivity(days));
    }

    @GetMapping("/cities")
    @Operation(summary = "Listings per city", description = "Active listings per city and listings reunited within the last N days (7–90).")
    @ApiResponse(responseCode = "200", description = "One entry per city",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CityOverviewDto.class))))
    public ResponseEntity<List<CityOverviewDto>> getCities(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminDashboardService.getCities(days));
    }
}
