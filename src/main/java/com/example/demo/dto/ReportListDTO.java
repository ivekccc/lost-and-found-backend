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
public class ReportListDTO {
    private Long id;
    private String title;
    private ReportType type;
    private String categoryName;
    private ReportStatus status;
    private String location;
    private LocalDateTime createdAt;
}
