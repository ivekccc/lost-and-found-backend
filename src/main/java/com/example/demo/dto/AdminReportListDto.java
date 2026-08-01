package com.example.demo.dto;

import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
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
public class AdminReportListDto {

    @NotNull
    private Long id;

    @NotBlank
    private String title;

    @NotNull
    private ReportType type;

    @NotBlank
    private String categoryName;

    @NotNull
    private ReportStatus status;

    private LocationDTO location;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private Long ownerId;

    private String ownerName;

    @Schema(description = "City the report belongs to, derived from its location zone. Absent for "
            + "reports created before location became mandatory — those are invisible in user-facing "
            + "search, which is exactly what this column makes visible to a moderator.")
    private String cityName;
}
