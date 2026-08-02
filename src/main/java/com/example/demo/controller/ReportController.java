package com.example.demo.controller;

import com.example.demo.dto.CommunityStatisticsDto;
import com.example.demo.dto.CreateReportRequestDto;
import com.example.demo.dto.MatchDto;
import com.example.demo.dto.NearbyReportDTO;
import com.example.demo.dto.ReportDetailsDTO;
import com.example.demo.dto.ReportListDTO;
import com.example.demo.model.ReportType;
import com.example.demo.model.TimeWindow;
import com.example.demo.service.CommunityStatisticsService;
import com.example.demo.service.ReportMatchService;
import com.example.demo.service.ReportService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report management endpoints")
public class ReportController {
    private final ReportService reportService;
    private final ReportMatchService reportMatchService;
    private final CommunityStatisticsService communityStatisticsService;

    @PostMapping
    @Operation(summary = "Create report", description = "Creates a new lost or found report")
    @ApiResponse(responseCode = "201", description = "Report created successfully")
    public ResponseEntity<ReportDetailsDTO> createReport(@Valid @RequestBody CreateReportRequestDto createReportRequestDto, @AuthenticationPrincipal UserDetails userDetails) {

        ReportDetailsDTO created = reportService.createReport(createReportRequestDto, userDetails.getUsername());
        return ResponseEntity.ok(created);
    }

    @GetMapping
    @Operation(summary = "Get all reports",
            description = "Active listings from other users in the caller's city, newest first. "
                    + "The search term is matched against both the title and the description, since the "
                    + "description is where colour, brand and the details that actually identify an item "
                    + "are written. All filters are optional and combine as an intersection. zoneId "
                    + "narrows to a part of the city and takes either level: given a municipality it "
                    + "also returns listings that resolved to a local community inside it, which is "
                    + "most of them.")
    @ApiResponse(responseCode = "200", description = "Reports returned",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ReportListDTO.class))))
    public ResponseEntity<List<ReportListDTO>> getReports(
            @RequestParam(required = false) ReportType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TimeWindow postedWithin,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long zoneId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ReportListDTO> reports = reportService.getReports(
                type, categoryId, postedWithin, search, zoneId, userDetails.getUsername());
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/mine")
    @Operation(summary = "Get my reports", description = "Returns the current user's own reports (all statuses except deleted)")
    public ResponseEntity<List<ReportListDTO>> getMyReports(@AuthenticationPrincipal UserDetails userDetails) {
        List<ReportListDTO> reports = reportService.getMyReports(userDetails.getUsername());
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/saved")
    @Operation(summary = "Get saved reports",
            description = "Listings the caller bookmarked for later, newest first. Deliberately NOT "
                    + "scoped to the caller's city — a saved listing is theirs, like their own reports, "
                    + "and switching cities must not hide something they set aside. Listings that were "
                    + "deleted or hidden by moderation drop out, since opening them would 404.")
    @ApiResponse(responseCode = "200", description = "Saved reports returned",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ReportListDTO.class))))
    public ResponseEntity<List<ReportListDTO>> getSavedReports(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.getSavedReports(userDetails.getUsername()));
    }

    @PostMapping("/{id}/save")
    @Operation(summary = "Save a report for later",
            description = "Idempotent: saving something already saved succeeds and changes nothing, so "
                    + "a double tap or a retried request cannot produce duplicates. You cannot save your "
                    + "own listing — it is already in My Reports.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Saved"),
            @ApiResponse(responseCode = "400", description = "Own listing"),
            @ApiResponse(responseCode = "404", description = "No such report, or not visible to you")
    })
    public ResponseEntity<Void> saveReport(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        reportService.saveReport(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/save")
    @Operation(summary = "Remove a report from saved",
            description = "Idempotent: succeeds even when the listing was not saved, because the outcome "
                    + "is the same either way.")
    @ApiResponse(responseCode = "204", description = "Removed from saved")
    public ResponseEntity<Void> unsaveReport(@PathVariable Long id,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        reportService.unsaveReport(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get found reports nearby",
            description = "Returns FOUND reports from other users, grouped by administrative zone rather than exact position. "
                    + "A zone is usually a local community of about 1 km², falling back to the city municipality where no finer "
                    + "unit covers the point. The caller's own zone is always included regardless of radiusKm, as is its parent "
                    + "municipality and any zone sharing that parent relationship, so a report just across a zone border is never "
                    + "dropped; other zones are included when their centroid lies within radiusKm of the given coordinates. "
                    + "Reports in the caller's own zone come first. distanceBand is measured to the report's ZONE CENTROID and is "
                    + "absent for reports in the caller's own zone.")
    public ResponseEntity<List<NearbyReportDTO>> getNearbyReports(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5") double radiusKm,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<NearbyReportDTO> reports = reportService.getNearbyReports(
                latitude, longitude, radiusKm, userDetails.getUsername());
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/statistics")
    @Operation(summary = "What the caller's city has done so far",
            description = "Counts for the city the caller is currently browsing: listings posted and "
                    + "listings their owner marked reunited, over the whole history rather than a "
                    + "recent window. Returns no content when the city has fewer reunions than the "
                    + "configured minimum, which is a normal answer rather than an error — a strip "
                    + "reading \"0 reunited\" argues that the app does not work, so the decision to "
                    + "stay silent is made here rather than in each client screen. Reunions are "
                    + "counted per listing, not per object: both sides of one reunion close their "
                    + "own listing, so the same object can count twice.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics for the caller's active city",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CommunityStatisticsDto.class))),
            @ApiResponse(responseCode = "204", description = "Too little activity to be worth showing")
    })
    public ResponseEntity<CommunityStatisticsDto> getCommunityStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {
        return communityStatisticsService.getForActiveCity(userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}/matches")
    @Operation(summary = "Get matches for my report",
            description = "Returns suggested matches for the given report, sorted by score descending. Only the report owner can access them.")
    public ResponseEntity<List<MatchDto>> getReportMatches(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<MatchDto> matches = reportMatchService.getMatchesForReport(id, userDetails.getUsername());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get report by ID", description = "Returns detailed information about a specific report. Found-report photos are only included for the report owner.")
    public ResponseEntity<ReportDetailsDTO> getReportById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reportService.getReportById(id, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete my report",
            description = "Removes the caller's own report from the platform. The row is kept so that "
                    + "verification history belonging to the other party stays intact, but the report is "
                    + "no longer visible anywhere, including to its owner. Any claim still awaiting a "
                    + "decision is declined and its claimant notified. Photos are not removed from storage "
                    + "here; deleting the account does that. Only the report owner may call this; another "
                    + "user's report is reported as not found.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Report deleted"),
            @ApiResponse(responseCode = "400", description = "Report is already deleted"),
            @ApiResponse(responseCode = "404", description = "Report not found or not owned by the caller")
    })
    public ResponseEntity<Void> deleteReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        reportService.deleteReport(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close my report",
            description = "Marks the caller's own active report as matched: it stops appearing in search, "
                    + "in nearby results and in the matching engine, and its existing matches become hidden "
                    + "to both sides. Nothing is deleted — the owner can reopen it later. Only the report "
                    + "owner may call this; another user's report is reported as not found.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report closed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReportDetailsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Report is not active"),
            @ApiResponse(responseCode = "404", description = "Report not found or not owned by the caller")
    })
    public ResponseEntity<ReportDetailsDTO> closeReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.closeReport(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Mark a report as reunited",
            description = "The owner confirms the item is back with its owner. This is the only "
                    + "path to RESOLVED, and the only outcome counted as a successful reunion — "
                    + "closing a listing without getting the item back stays MATCHED so the number "
                    + "is not inflated. Allowed from active and closed listings, since approving a "
                    + "claim often closes a found listing before the handover happens. Any claims "
                    + "still awaiting a decision are declined, and whoever had an approved claim is "
                    + "notified.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Marked as reunited",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReportDetailsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Report is not active or closed"),
            @ApiResponse(responseCode = "404", description = "No such report, or not yours")
    })
    public ResponseEntity<ReportDetailsDTO> resolveReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.resolveReport(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reopen my report",
            description = "Returns the caller's own closed report to active: it appears in search and nearby "
                    + "again, re-enters the matching engine, and its previously hidden matches become visible "
                    + "immediately. The expiry date is extended by a full term, so a report that sat closed "
                    + "past its original expiry is not swept away right after being reopened. Only the report "
                    + "owner may call this; another user's report is reported as not found.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report reopened",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReportDetailsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Report is not closed"),
            @ApiResponse(responseCode = "404", description = "Report not found or not owned by the caller")
    })
    public ResponseEntity<ReportDetailsDTO> reopenReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.reopenReport(id, userDetails.getUsername()));
    }
}
