package com.example.demo.dto;

import com.example.demo.model.DistanceBand;
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
public class NearbyReportDTO {

    @NotNull
    private Long id;

    @NotBlank
    private String title;

    @NotNull
    private ReportType type;

    @NotBlank
    private String categoryName;

    private String categoryImageUrl;

    @NotNull
    private ReportStatus status;

    private LocationDTO location;

    @NotNull
    private LocalDateTime createdAt;

    private String thumbnailUrl;

    @NotNull
    private Boolean reported;

    @Schema(description = "How far the report's zone is from the given coordinates, as a band rather "
            + "than a number. Measured to the CENTROID of the zone, never to the exact location, so a "
            + "one-decimal figure would look far more precise than it is. Null when the report is in "
            + "the caller's own zone or in its parent municipality, where a centroid distance would be "
            + "misleading.")
    private DistanceBand distanceBand;
}
