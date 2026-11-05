package com.example.demo.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Address suggestion for report creation.")
public class AutoCompleteSuggestionDTO {

    @NotBlank
    private String osmId;

    @NotBlank
    private String osmType;

    @NotBlank
    private String displayName;

    // LocationIQ does not return the split display form for every result type, so these stay optional.
    private String displayPlace;

    private String displayAddress;

    @NotNull
    @Schema(description = "Latitude of the address, where the map picker opens")
    private Double latitude;

    @NotNull
    @Schema(description = "Longitude of the address, where the map picker opens")
    private Double longitude;
}
