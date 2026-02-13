package com.example.demo.controller;

import com.example.demo.dto.ReportCategoryDto;
import com.example.demo.service.ReportCategoryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/report-categories")
@RequiredArgsConstructor
@Tag(name="Report Categories",description = "Report categories endpoints")
public class ReportCategoryController {

    private final ReportCategoryService reportCategoryService;

    @GetMapping
    @Operation(summary = "Get all categories", description = "Returns a list of all active report categories")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved categories",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ReportCategoryDto.class))
                    )
            )
    })
    public ResponseEntity<List<ReportCategoryDto>> getAllCategories() {
        return ResponseEntity.ok(reportCategoryService.getAllActiveCategories());
    }
}
