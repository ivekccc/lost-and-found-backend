package com.example.demo.location;

import com.example.demo.location.dto.LocationRequestDTO;
import com.example.demo.location.model.City;
import com.example.demo.location.dto.CityDto;
import com.example.demo.location.dto.ReportZoneDto;
import com.example.demo.location.model.Location;
import com.example.demo.location.model.Zone;

/**
 * The only entry point other modules may use to reach places.
 *
 * {@code findOrCreate} exists as one call rather than a lookup plus a save because deduplicating
 * by OSM id and resolving the zone belong together — a location saved without its zone would drop
 * out of search and matching without any visible sign.
 */
public interface LocationFacade {

    Location findOrCreate(LocationRequestDTO request);

    City requireByCode(String code);

    City requireActiveCityOf(String userEmail);

    City setActiveCity(String userEmail, Long cityId);

    java.util.Optional<ReportZoneDto> describeZone(Long zoneId);

    java.util.List<CityDto> listActiveCities();

    java.util.Optional<Zone> resolveZone(java.math.BigDecimal latitude, java.math.BigDecimal longitude);

    CityDto toCityDto(City city);
}
