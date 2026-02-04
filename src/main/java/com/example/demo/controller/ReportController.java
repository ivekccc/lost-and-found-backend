package com.example.demo.controller;

import com.example.demo.dto.ReportDetailsDTO;
import com.example.demo.dto.ReportListDTO;
import com.example.demo.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report management endpoints")
public class ReportController {
    private final ReportService reportService;

    @GetMapping
    @Operation(summary = "Get all reports", description = "Returns a list of all active reports")
//     @ApiResponse(
//     responseCode = "200", 
//     description = "Successfully retrieved list",
//     content = @Content(
//         mediaType = "application/json",
//         array = @ArraySchema(schema = @Schema(implementation = ReportListDTO.class))
//     )
// )
    public ResponseEntity<List<ReportListDTO>> getAllReports() {
        List<ReportListDTO> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get report by ID", description = "Returns detailed information about a specific report")
    public ResponseEntity<ReportDetailsDTO> getReportById(@PathVariable Long id) {
        return reportService.getReportById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
