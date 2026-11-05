package com.example.demo.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportImageRequestDTO {

    @NotBlank(message = "{validation.image.url.required}")
    @Size(max = 500)
    private String imageUrl;

    @NotBlank(message = "{validation.image.publicId.required}")
    @Size(max = 255)
    private String publicId;
}
