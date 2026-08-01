package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CommunityStatisticsDto",
        description = "What the caller's city has done so far. Counted over the city's whole history "
                + "rather than a recent window, because the smaller cities would show a zero for any "
                + "window short enough to feel current, and a zero argues that the app does not work.")
public class CommunityStatisticsDto {

    @NotBlank
    @Schema(description = "Name of the caller's active city, so the client never has to join this "
            + "against the profile to write the sentence.",
            example = "Beograd")
    private String cityName;

    @NotNull
    @Schema(description = "Listings posted in this city. Includes the caller's own, unlike the "
            + "search list which hides them, so this number is not the length of any list the user "
            + "sees.")
    private Long reportsPosted;

    @NotNull
    @Schema(description = "Listings whose owner marked them reunited. Counted PER LISTING, not per "
            + "object: when both the person who lost a wallet and the person who found it close "
            + "their own listing, the same wallet counts twice. Client copy must therefore say "
            + "\"reports\", never \"items\".")
    private Long reportsReunited;
}
