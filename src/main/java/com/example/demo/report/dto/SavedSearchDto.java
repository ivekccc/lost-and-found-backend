package com.example.demo.report.dto;

import com.example.demo.report.model.ReportType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SavedSearchDto",
        description = "Search the user is notified about when a matching report is posted. Absent "
                + "filters match everything.")
public class SavedSearchDto {

    @NotNull
    private Long id;

    @NotNull
    private Long cityId;

    @NotBlank
    private String cityName;

    private ReportType type;

    private Long categoryId;

    @Schema(description = "Category name in the caller's language.")
    private String categoryName;

    private Long zoneId;

    private String zoneName;

    @Schema(description = "Municipality the saved zone belongs to; absent when the zone is a municipality.")
    private Long zoneParentId;

    private String zoneParentName;

    private String search;

    @NotNull
    private LocalDateTime createdAt;
}
