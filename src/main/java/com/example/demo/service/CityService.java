package com.example.demo.service;

import com.example.demo.dto.CityDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.City;
import com.example.demo.model.User;
import com.example.demo.repository.CityRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CityDto> listActiveCities() {
        return cityRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(CityService::toDto)
                .toList();
    }

    /**
     * Grad kome pripada data tacka, ako je uopste pokriven.
     *
     * Prazan rezultat NIJE greska: korisnik u gradu koji aplikacija ne pokriva jednostavno
     * ne dobija predlog. Zato ovo vraca Optional, a endpoint 204 umesto 404.
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
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));

        // Neaktivan grad je onaj cije zone jos nisu spremne. Da se moze izabrati, korisnik bi
        // dobio praznu pretragu bez ijednog objasnjenja i to bi izgledalo kao kvar.
        if (!Boolean.TRUE.equals(city.getActive())) {
            throw new IllegalArgumentException("This city is not available yet");
        }

        user.setActiveCity(city);
        return city;
    }

    /**
     * Grad korisnika, kao pun entitet — zove ga LocationService kome treba okvir za pretragu
     * adresa. Ucitava se jednim upitom, ne kroz {@code user.getActiveCity()} proxy: vidi
     * {@link CityRepository#findActiveCityByUserEmail}.
     */
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
