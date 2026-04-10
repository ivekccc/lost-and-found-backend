package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportImageRequestDTO {

    @NotBlank(message = "Image URL is required")
    @Size(max = 500)
    private String imageUrl;

    @NotBlank(message = "Public ID is required")
    @Size(max = 255)
    private String publicId;
}