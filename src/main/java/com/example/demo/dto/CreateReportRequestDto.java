package com.example.demo.dto;

import com.example.demo.model.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateReportRequest", description = "Request body for creating a new report")
public class CreateReportRequestDto {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;

    @NotNull(message = "Type is required")
    private ReportType type;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @Size(max = 500, message = "Location must be less than 500 characters")
    private String location;

    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String contactEmail;

    @Size(max = 50, message = "Phone must be less than 50 characters")
    private String contactPhone;
}
