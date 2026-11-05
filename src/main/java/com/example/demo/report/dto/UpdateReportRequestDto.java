package com.example.demo.report.dto;

import com.example.demo.location.dto.LocationRequestDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateReportRequestDto", description = "The owner's edit of a published report. "
        + "Type, category and verification questions cannot be changed.")
public class UpdateReportRequestDto {

    @NotBlank(message = "{validation.report.title.required}")
    @Size(max = 255, message = "{validation.report.title.size}")
    private String title;

    @Size(max = 2000, message = "{validation.report.description.size}")
    private String description;

    @Valid
    @NotNull(message = "{validation.report.location.required}")
    private LocationRequestDTO location;

    @Email(message = "{validation.email.invalid}")
    @Size(max = 255)
    private String contactEmail;

    @Size(max = 50, message = "{validation.report.phone.size}")
    private String contactPhone;

    @NotNull
    @Schema(description = "Ids of the current photos to keep, in display order; photos left out are removed")
    private List<Long> keptImageIds = new ArrayList<>();

    @Valid
    @Schema(description = "Newly uploaded photos, shown after the kept ones")
    private List<ReportImageRequestDTO> newImages = new ArrayList<>();
}
