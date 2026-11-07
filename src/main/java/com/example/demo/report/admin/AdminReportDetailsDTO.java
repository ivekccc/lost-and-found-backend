package com.example.demo.report.admin;

import com.example.demo.location.dto.LocationDTO;
import com.example.demo.report.dto.ReportImageDTO;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportDetailsDTO {

    @NotNull
    private Long id;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private ReportType type;

    @NotNull
    private Long categoryId;

    @NotBlank
    private String categoryName;

    @NotNull
    private ReportStatus status;

    private LocationDTO location;

    @NotNull
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    @NotNull
    private Long userId;

    private String userFullName;

    private String contactEmail;

    private String contactPhone;

    private List<ReportImageDTO> images;

    private Integer rewardAmount;
}
