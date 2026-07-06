package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryImageRequestDto {

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    @NotBlank(message = "Image public id is required")
    private String imagePublicId;
}
