package com.example.demo.location.dto;

import com.example.demo.location.model.Location;
import com.example.demo.location.model.Zone;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Location data")
public class LocationDTO {

    @Schema(description = "Exact latitude. Null for everyone except the report owner "
            + "(and after their ownership claim is approved) — see the zonal location view.")
    private Double latitude;

    @Schema(description = "Exact longitude. Null for everyone except the report owner "
            + "(and after their ownership claim is approved) — see the zonal location view.")
    private Double longitude;

    @Schema(description = "Full street address for the owner; just the zone name "
            + "(e.g. \"Mirijevo, Zvezdara\") for everyone else.")
    private String formattedAddress;

    public static LocationDTO fromEntity(Location location){
        if(location == null){
            return null;
        }
        return LocationDTO.builder().latitude(location.getLatitude().doubleValue())
                .longitude(location.getLongitude().doubleValue())
                .formattedAddress(location.getFormattedAddress()).build();
    }

    /** Zonal view: no coordinates, no full address, only the zone name. */
    public static LocationDTO zonalFromEntity(Location location) {
        if (location == null) {
            return null;
        }
        return LocationDTO.builder().formattedAddress(zoneLabel(location)).build();
    }

    /**
     * Zone label: "Mirijevo, Zvezdara" for a level-2 zone, "Zvezdara, Beograd" for a municipality.
     * The second part is dropped when it repeats the first, or Bajina Bašta reads twice.
     */
    public static String zoneLabel(Location location) {
        Zone zone = location.getZone();
        if (zone == null) {
            return location.getCity() != null ? location.getCity() : "Nepoznata lokacija";
        }
        return zoneLabel(zone);
    }

    public static String zoneLabel(Zone zone) {
        String parent = zone.getParentName() != null ? zone.getParentName() : zone.getCity();
        if (parent == null || parent.equals(zone.getName())) {
            return zone.getName();
        }
        return zone.getName() + ", " + parent;
    }
}
