package com.example.demo.location.service;

import com.example.demo.location.dto.CityDto;
import com.example.demo.location.model.City;
import com.example.demo.location.repository.CityRepository;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.model.User;
import com.example.demo.user.UserFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final UserFacade userFacade;

    @Transactional(readOnly = true)
    public List<CityDto> listActiveCities() {
        return cityRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(CityService::toDto)
                .toList();
    }

    /**
     * The city a point falls in, if any. An empty result is NOT an error: someone in a city the app
     * does not cover simply gets no suggestion, which is why this returns Optional and the endpoint
     * answers 204 rather than 404.
     */
    @Transactional(readOnly = true)
    public Optional<CityDto> detectCity(double latitude, double longitude) {
        if (!isValidCoordinate(latitude, 90) || !isValidCoordinate(longitude, 180)) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90, longitude between -180 and 180");
        }
        return cityRepository.findCityContaining(latitude, longitude).map(CityService::toDto);
    }

    @Transactional
    public City setActiveCity(String userEmail, Long cityId) {
        User user = userFacade.requireByEmail(userEmail);
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));

        // An inactive city is one whose zones are not ready. Allowing it would give the user an empty
        // search with no explanation, which reads as a fault.
        if (!Boolean.TRUE.equals(city.getActive())) {
            throw new IllegalArgumentException("This city is not available yet");
        }

        user.setActiveCity(city);
        return city;
    }

    /** The user's city as a full entity in one query, not through the lazy proxy. */
    @Transactional(readOnly = true)
    public City getActiveCity(String userEmail) {
        return cityRepository.findActiveCityByUserEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public static CityDto toDto(City city) {
        return new CityDto(
                city.getId(),
                city.getCode(),
                city.getName(),
                city.getCenterLatitude(),
                city.getCenterLongitude()
        );
    }

    private boolean isValidCoordinate(double value, double bound) {
        return !Double.isNaN(value) && value >= -bound && value <= bound;
    }
}
