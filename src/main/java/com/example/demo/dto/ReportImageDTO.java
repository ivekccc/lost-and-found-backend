package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportImageDTO {

    @NotNull
    private Long id;

    @NotBlank
    private String imageUrl;

    @NotNull
    private Integer displayOrder;
}