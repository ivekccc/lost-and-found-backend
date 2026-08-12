package com.example.demo.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * An address suggestion from LocationIQ. osmId and osmType are required because a listing is
 * created from that pair — clients used to cast them because the generated type had them optional.
 */
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
}
