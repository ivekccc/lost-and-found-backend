package com.example.demo.location.service;

import com.example.demo.location.dto.CityZoneDto;
import com.example.demo.location.dto.ReportZoneDto;
import com.example.demo.location.model.City;
import com.example.demo.location.model.Zone;
import com.example.demo.location.repository.ZoneRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class ZoneService {

    private static final short AREA_LEVEL = 1;
    private static final short NEIGHBOURHOOD_LEVEL = 2;
    private static final int MAX_PAGE_SIZE = 50;

    private final ZoneRepository zoneRepository;
    private final CityService cityService;

    /** The deepest zone containing the point, or empty when it falls outside every covered city. */
    @Transactional(readOnly = true)
    public Optional<Zone> resolveZone(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return Optional.empty();
        }

        return zoneRepository
                .findZoneIdContaining(latitude.doubleValue(), longitude.doubleValue())
                .flatMap(zoneRepository::findById);
    }

    /**
     * Zone boundary as GeoJSON for the map on a listing. Takes an id, not an entity, so reading it
     * does not initialise the lazy Zone proxy.
     */
    @Transactional(readOnly = true)
    public Optional<ReportZoneDto> describeZone(Long zoneId) {
        if (zoneId == null) {
            return Optional.empty();
        }

        return zoneRepository.findZoneBoundary(zoneId)
                .map(zone -> new ReportZoneDto(
                        zone.getName(), zone.getCity(), zone.getParentName(),
                        zone.getBoundaryGeoJson()));
    }

    /**
     * The coarse level of the caller's city: 17 municipalities in Belgrade, but a single unit in
     * Novi Sad and Bajina Bašta. A list shorter than two tells the client not to show the control —
     * so no client anywhere reasons about which city it is in.
     */
    @Transactional(readOnly = true)
    public List<CityZoneDto> getAreas(String userEmail) {
        City city = cityService.getActiveCity(userEmail);
        return zoneRepository.findByCityIdAndLevelOrderByNameAsc(city.getId(), AREA_LEVEL).stream()
                .map(ZoneService::toDto)
                .toList();
    }

    /**
     * The fine level, optionally narrowed to one area. areaId is optional on purpose: the picker
     * works across the whole city too, which is the only shape the smaller cities use.
     */
    @Transactional(readOnly = true)
    public Page<CityZoneDto> getNeighbourhoods(String userEmail, Long areaId, String search,
                                               int page, int size) {
        City city = cityService.getActiveCity(userEmail);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        String pattern = (search == null || search.isBlank())
                ? "%"
                : "%" + search.trim() + "%";

        return zoneRepository
                .findFilterZones(city.getId(), NEIGHBOURHOOD_LEVEL, areaId, pattern, pageable)
                .map(ZoneService::toDto);
    }

    private static CityZoneDto toDto(Zone zone) {
        return new CityZoneDto(
                zone.getId(), zone.getName(), zone.getParentId(), zone.getParentName());
    }
}
