package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CityDto",
        description = "A city the app has zone coverage for. Picking one scopes search, the nearby "
                + "feed, address lookup and matching to that city.")
public class CityDto {

    @NotNull
    private Long id;

    @NotBlank
    @Schema(description = "Stable short identifier, also used as the prefix of this city's zone "
            + "codes (\"BG\" for Beograd). Safe to compare against; the name is not.",
            example = "NS")
    private String code;

    @NotBlank
    @Schema(example = "Novi Sad")
    private String name;

    @NotNull
    @Schema(description = "Point guaranteed to lie inside the city, for centring a map when the "
            + "user has no location fix.")
    private BigDecimal centerLatitude;

    @NotNull
    private BigDecimal centerLongitude;
}
