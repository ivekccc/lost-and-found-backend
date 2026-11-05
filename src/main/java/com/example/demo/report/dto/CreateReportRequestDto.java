package com.example.demo.report.dto;

import com.example.demo.location.dto.LocationRequestDTO;
import com.example.demo.location.model.Location;
import com.example.demo.report.model.ReportType;
import com.example.demo.verification.dto.ChallengeQuestionRequestDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateReportRequest", description = "Request body for creating a new report")
public class CreateReportRequestDto {
    @NotBlank(message = "{validation.report.title.required}")
    @Size(max = 255, message = "{validation.report.title.size}")
    private String title;

    @Size(max = 2000, message = "{validation.report.description.size}")
    private String description;

    @NotNull(message = "{validation.report.type.required}")
    private ReportType type;

    @NotNull(message = "{validation.category.required}")
    private Long categoryId;

    @Valid
    @NotNull(message = "{validation.report.location.required}")
    private LocationRequestDTO location;

    @Email(message = "{validation.email.invalid}")
    @Size(max = 255)
    private String contactEmail;

    @Size(max = 50, message = "{validation.report.phone.size}")
    private String contactPhone;

    @Valid
    @Size(max = 5, message = "{validation.report.images.size}")
    private List<ReportImageRequestDTO> images;

    @Valid
    @Size(max = 10, message = "{validation.report.questions.size}")
    private List<ChallengeQuestionRequestDto> questions;
}
