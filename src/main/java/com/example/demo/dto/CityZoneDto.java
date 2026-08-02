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
@Schema(name = "CityZoneDto",
        description = "An administrative area of the caller's city, offered as a search filter. "
                + "The two levels are the same shape on purpose: which level a city actually has is "
                + "a property of that city, not something the client should reason about.")
public class CityZoneDto {

    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "Zvezdara")
    private String name;

    @Schema(description = "The coarser area this one sits in. Null for the coarse level itself. "
            + "Present on the fine level so that picking a neighbourhood can fill in the area it "
            + "belongs to, without a second request.")
    private Long parentId;

    @Schema(description = "Name of the parent area, for telling apart same-named neighbourhoods "
            + "from different municipalities. Null when the parent's name repeats this one.",
            example = "Zvezdara")
    private String parentName;
}
