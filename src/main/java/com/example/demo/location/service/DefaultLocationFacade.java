package com.example.demo.location.service;

import com.example.demo.location.LocationFacade;
import com.example.demo.location.dto.CityDto;
import com.example.demo.location.dto.LocationRequestDTO;
import com.example.demo.location.dto.ReportZoneDto;
import com.example.demo.location.model.City;
import com.example.demo.location.model.Location;
import com.example.demo.location.model.Zone;

import java.math.BigDecimal;
import com.example.demo.location.repository.CityRepository;

import java.util.List;
import java.util.Optional;
import com.example.demo.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultLocationFacade implements LocationFacade {

    private final LocationRepository locationRepository;
    private final CityRepository cityRepository;
    private final LocationService locationService;
    private final ZoneService zoneService;
    private final CityService cityService;

    @Override
    @Transactional
    public Location findOrCreate(LocationRequestDTO request) {
        return locationRepository.findByOsmId(request.getOsmId())
                .orElseGet(() -> {
                    Location location = locationService.lookupLocation(
                            request.getOsmId(), request.getOsmType());
                    location.setZone(zoneService
                            .resolveZone(location.getLatitude(), location.getLongitude())
                            .orElse(null));
                    return locationRepository.save(location);
                });
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
