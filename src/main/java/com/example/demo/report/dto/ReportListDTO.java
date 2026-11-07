package com.example.demo.report.dto;

import com.example.demo.location.dto.LocationDTO;
import com.example.demo.report.model.ReportStatus;
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
public class ReportListDTO {

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

    private Long matchCount;

    @Schema(description = "Reward in dinars the owner offers to the finder; only ever set on LOST reports.")
    private Integer rewardAmount;
}
