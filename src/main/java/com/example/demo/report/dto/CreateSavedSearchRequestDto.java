package com.example.demo.report.dto;

import com.example.demo.report.model.ReportType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "CreateSavedSearchRequestDto",
        description = "Search filters to be notified about. At least one must be set; the city is "
                + "always the caller's active city.")
public class CreateSavedSearchRequestDto {

    private ReportType type;

    private Long categoryId;

    @Schema(description = "Zone in the active city; a municipality also covers its local communities.")
    private Long zoneId;

    @Size(max = 100, message = "{validation.savedSearch.search.size}")
    private String search;
}
