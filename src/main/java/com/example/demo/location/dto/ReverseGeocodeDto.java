package com.example.demo.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The address under a pair of device coordinates.
 *
 * The suggestion is returned even when the point falls outside the city the caller is browsing, so
 * the app can offer to switch city and then use the address straight away — LocationIQ is rate
 * limited to a couple of requests a second, and a second round trip for the same point would be
 * both slower and easy to trip.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReverseGeocodeDto", description = "Address resolved from coordinates, with the city it falls in")
public class ReverseGeocodeDto {

    @Schema(description = "The nearest address, in the same shape autocomplete returns. Null when there is "
            + "no address at the point, such as inside a park — the point itself is still valid")
    private AutoCompleteSuggestionDTO suggestion;

    @NotNull
    @Schema(description = "The supported city the point falls in")
    private CityDto city;

    @NotNull
    @Schema(description = "False when the point is in a different city than the one the caller is browsing; "
            + "the client then has to switch city before the point can be used for a listing")
    private Boolean matchesActiveCity;
}
