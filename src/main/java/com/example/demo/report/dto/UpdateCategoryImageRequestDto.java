package com.example.demo.report.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryImageRequestDto {

    @NotBlank(message = "{validation.image.url.required}")
    private String imageUrl;

    @NotBlank(message = "{validation.image.publicId.required}")
    private String imagePublicId;
}
