package com.example.demo.controller;

import com.example.demo.dto.AdminReportDetailsDTO;
import com.example.demo.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports", description = "Admin endpoints for managing reports")
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping("/{id}")
    @Operation(summary = "Get report by id", description = "Returns full report details including contact info")
    public ResponseEntity<AdminReportDetailsDTO> getReportById(@PathVariable Long id) {
        AdminReportDetailsDTO report = adminReportService.getReportById(id);
        return ResponseEntity.ok(report);
    }
}
