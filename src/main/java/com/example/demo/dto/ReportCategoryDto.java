package com.example.demo.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name="ReportCategoryDto", description = "Report category information")
public class ReportCategoryDto {
    private Long id;
    private String name;
}
