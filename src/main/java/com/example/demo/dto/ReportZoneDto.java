package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReportZoneDto",
        description = "Administrative zone a report belongs to, with its boundary for map display. "
                + "Usually a local community (mesna zajednica) or settlement of about 1 km²; falls "
                + "back to a coarser unit — a city municipality, or the whole city where the city has "
                + "no municipalities — where no finer unit covers the point, "
                + "such as parks, riverbanks and industrial land.")
public class ReportZoneDto {

    @NotBlank
    private String name;

    @NotBlank
    private String city;

    @Schema(description = "Name of the containing city municipality, e.g. \"Zvezdara\" for the "
            + "\"Mirijevo\" zone. Absent when the zone IS a municipality, which happens where no "
            + "finer unit covers the location.")
    private String parentName;

    @Schema(description = "Zone boundary as a GeoJSON MultiPolygon geometry object. "
            + "Always a MultiPolygon, never a bare Polygon, so clients need a single parsing path. "
            + "Zones larger than 2 km² are simplified for display; smaller ones are sent as stored, "
            + "because display-level simplification would visibly deform a polygon that small. The "
            + "threshold is on area rather than administrative level, since a rural settlement can be "
            + "far larger than a city municipality.")
    @NotBlank
    private String boundaryGeoJson;
}
