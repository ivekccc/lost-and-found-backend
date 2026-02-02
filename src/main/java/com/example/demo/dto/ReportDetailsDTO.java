package com.example.demo.dto;

import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDetailsDTO {
    private Long id;
    private String title;
    private String description;
    private ReportType type;
    private Long categoryId;
    private String categoryName;
    private ReportStatus status;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Long userId;
    private String contactEmail;
    private String contactPhone;
}
