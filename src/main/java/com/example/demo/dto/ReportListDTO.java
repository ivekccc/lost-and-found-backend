package com.example.demo.dto;

import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
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

    @NotNull
    private ReportStatus status;



    @NotNull
    private LocalDateTime createdAt;
}
