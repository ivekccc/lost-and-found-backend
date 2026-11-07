package com.example.demo.location.service;

import com.example.demo.location.LocationFacade;
import com.example.demo.location.dto.CityDto;
import com.example.demo.location.dto.LocationDTO;
import com.example.demo.location.dto.ReportZoneDto;
import com.example.demo.location.model.City;
import com.example.demo.location.model.Location;
import com.example.demo.location.model.Zone;
import com.example.demo.location.repository.CityRepository;
import com.example.demo.location.repository.LocationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultLocationFacade implements LocationFacade {

    private final LocationRepository locationRepository;
    private final CityRepository cityRepository;
    private final LocationService locationService;
    private final ZoneService zoneService;
    private final CityService cityService;

    private static final int COORDINATE_SCALE = 8;

    @Override
    @Transactional
    public void deleteLocation(Location location) {
        if (location != null) {
            locationRepository.delete(location);
        }
    }

    @Override
    @Transactional
    public Location createAt(BigDecimal latitude, BigDecimal longitude) {
        Zone zone = zoneService.resolveZone(latitude, longitude).orElse(null);
        Location location = zone == null
                ? Location.builder().build()
                : locationService.describePoint(latitude.doubleValue(), longitude.doubleValue());
        location.setLatitude(latitude.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP));
        location.setLongitude(longitude.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP));
        location.setZone(zone);
        if (location.getFormattedAddress() == null) {
            location.setFormattedAddress(LocationDTO.zoneLabel(location));
        }
        return locationRepository.save(location);
    }

    @Override
    @Transactional(readOnly = true)
    public City requireByCode(String code) {
        return cityRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException(
                        "Default city '" + code + "' is missing from the cities table"));
    }

    @Override
    @Transactional(readOnly = true)
    public City requireActiveCityOf(String userEmail) {
        return cityService.getActiveCity(userEmail);
    }

    @Override
    @Transactional
    public City setActiveCity(String userEmail, Long cityId) {
        return cityService.setActiveCity(userEmail, cityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportZoneDto> describeZone(Long zoneId) {
        return zoneService.describeZone(zoneId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> describeMeetingPoint(BigDecimal latitude, BigDecimal longitude, Long cityId) {
        Optional<Zone> zone = zoneService.resolveZone(latitude, longitude)
                .filter(found -> found.getCityId().equals(cityId));
        if (zone.isEmpty()) {
            return Optional.empty();
        }
        Location described = locationService.describePoint(latitude.doubleValue(), longitude.doubleValue());
        described.setZone(zone.get());
        String area = LocationDTO.zoneLabel(described);
        return Optional.of(described.getStreet() == null || described.getStreet().isBlank()
                ? area
                : described.getStreet() + ", " + area);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Zone> findZone(Long zoneId) {
        return zoneService.findById(zoneId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityDto> listActiveCities() {
        return cityService.listActiveCities();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Zone> resolveZone(BigDecimal latitude, BigDecimal longitude) {
        return zoneService.resolveZone(latitude, longitude);
    }

    @Override
    public CityDto toCityDto(City city) {
        return CityService.toDto(city);
    }
}
