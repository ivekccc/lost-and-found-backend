package com.example.demo.dto;


import com.example.demo.model.Location;
import com.example.demo.model.Zone;
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

    /**
     * Zonalni prikaz lokacije: bez koordinata i bez pune adrese, samo naziv zone.
     * Koristi se za sve koji nisu vlasnik oglasa (privacy by design) — vidi
     * ReportService.locationFor.
     */
    public static LocationDTO zonalFromEntity(Location location) {
        if (location == null) {
            return null;
        }
        return LocationDTO.builder().formattedAddress(zoneLabel(location)).build();
    }

    /**
     * Labela zone: "Mirijevo, Zvezdara" za zonu nivoa 2, "Zvezdara, Beograd" za samu
     * opstinu (tamo gde finija jedinica ne postoji).
     *
     * Naziv roditelja se cita iz denormalizovane kolone zones.parent_name, a ne kroz
     * asocijaciju ili zaseban upit: ova metoda se poziva za SVAKI oglas u listi, pa bi
     * svaki drugi pristup bio N+1.
     */
    private static String zoneLabel(Location location) {
        Zone zone = location.getZone();
        if (zone == null) {
            return location.getCity() != null ? location.getCity() : "Nepoznata lokacija";
        }
        String parent = zone.getParentName() != null ? zone.getParentName() : zone.getCity();
        return zone.getName() + ", " + parent;
    }
}
