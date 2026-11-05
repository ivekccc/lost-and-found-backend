package com.example.demo.location.service;

import com.example.demo.location.dto.AutoCompleteSuggestionDTO;
import com.example.demo.location.dto.CityDto;
import com.example.demo.location.dto.LocationDTO;
import com.example.demo.location.dto.ReverseGeocodeDto;
import com.example.demo.location.dto.locationiq.LocationIqAddress;
import com.example.demo.location.dto.locationiq.LocationIqResult;
import com.example.demo.location.model.City;
import com.example.demo.location.model.Location;
import com.example.demo.shared.exception.LocationOutsideSupportedCitiesException;
import com.example.demo.shared.exception.RateLimitExceededException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    // More candidates are fetched than shown, because the zone filter discards some of them.
    private static final int CANDIDATE_LIMIT = 15;
    private static final int SUGGESTION_LIMIT = 5;

    // LocationIQ throttles at a couple of requests a second; a short wait is genuinely enough.
    private static final int LOCATIONIQ_RETRY_AFTER_SECONDS = 2;

    @Value("${locationiq.api-key}")
    private String apiKey;

    @Value("${locationiq.base-url}")
    private String baseUrl;

    @Value("${locationiq.default-country}")
    private String defaultCountry;

    private final RestTemplate restTemplate;
    private final CityService cityService;
    private final ZoneService zoneService;

    /**
     * Address suggestions, limited to the city the caller is browsing.
     *
     * The viewbox is a RECTANGLE and cities are not: Belgrade's box reaches across the Danube into
     * Pančevo, Stara Pazova and Ruma. Suggestions are therefore filtered through the same
     * zone_resolve the listing later uses, so the two cannot disagree — otherwise the user picks an
     * address, fills the whole form, and only then gets a 400.
     */
    public List<AutoCompleteSuggestionDTO> getAutoCompleteSuggestions(String query, String userEmail) {
        City city = cityService.getActiveCity(userEmail);
        String viewbox = city.getBboxMinLongitude() + "," + city.getBboxMaxLatitude()
                + "," + city.getBboxMaxLongitude() + "," + city.getBboxMinLatitude();
        String url = baseUrl + "/autocomplete"
                + "?key=" + apiKey
                + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&countrycodes=" + defaultCountry
                + "&viewbox=" + viewbox
                + "&bounded=1"
                + "&limit=" + CANDIDATE_LIMIT
                + "&accept-language=" + addressLanguage();
        try {
            LocationIqResult[] results = restTemplate.getForObject(url, LocationIqResult[].class);
            if (results == null || results.length == 0) {
                return Collections.emptyList();
            }
            // The stream is lazy, so zones are resolved only until the limit is reached.
            return Arrays.stream(results)
                    .filter(result -> liesInCity(result, city))
                    .limit(SUGGESTION_LIMIT)
                    .map(this::toAutoCompleteDTO)
                    .toList();
        } catch (HttpClientErrorException.TooManyRequests | HttpClientErrorException.NotFound e) {
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("LocationIQ API error: " + e.getMessage(), e);
        }
    }

    public ReverseGeocodeDto reverseGeocode(double latitude, double longitude, String userEmail) {
        if (!isValidCoordinate(latitude, 90) || !isValidCoordinate(longitude, 180)) {
            throw new IllegalArgumentException(
                    "location.coordinatesOutOfRange");
        }

        City activeCity = cityService.getActiveCity(userEmail);
        CityDto city = cityService
                .detectCity(latitude, longitude)
                .orElseThrow(() -> new LocationOutsideSupportedCitiesException(
                        "location.outsideSupportedCities"));

        AutoCompleteSuggestionDTO suggestion = fetchAddress(latitude, longitude)
                .map(this::toAutoCompleteDTO)
                .orElse(null);
        if (suggestion != null) {
            zoneService.resolveZone(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude))
                    .map(LocationDTO::zoneLabel)
                    .ifPresent(suggestion::setDisplayAddress);
        }

        return new ReverseGeocodeDto(
                suggestion,
                city,
                city.getId().equals(activeCity.getId()));
    }

    public Location describePoint(double latitude, double longitude) {
        try {
            return fetchAddress(latitude, longitude)
                    .map(this::toLocation)
                    .orElseGet(() -> Location.builder().build());
        } catch (RuntimeException e) {
            log.warn("LocationIQ could not describe {},{}: {}", latitude, longitude, e.getMessage());
            return Location.builder().build();
        }
    }

    private Optional<LocationIqResult> fetchAddress(double latitude, double longitude) {
        String url = baseUrl + "/reverse"
                + "?key=" + apiKey
                + "&lat=" + latitude
                + "&lon=" + longitude
                + "&format=json"
                + "&addressdetails=1"
                + "&accept-language=" + addressLanguage();

        LocationIqResult result;
        try {
            // Unlike /autocomplete and /lookup, /reverse answers with a single object.
            result = restTemplate.getForObject(url, LocationIqResult.class);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new RateLimitExceededException(
                    "location.serviceBusy",
                    LOCATIONIQ_RETRY_AFTER_SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("LocationIQ API error: " + e.getMessage(), e);
        }

        if (result == null || parseCoordinate(result.getLat()) == null
                || parseCoordinate(result.getLon()) == null) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    private boolean isValidCoordinate(double value, double bound) {
        return !Double.isNaN(value) && Math.abs(value) <= bound;
    }

    private boolean liesInCity(LocationIqResult result, City city) {
        if (result.getLat() == null || result.getLon() == null) {
            return false;
        }
        try {
            return cityService
                    .detectCity(Double.parseDouble(result.getLat()),
                            Double.parseDouble(result.getLon()))
                    .map(detected -> detected.getId().equals(city.getId()))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            // Covers both an unparsable value and an out-of-range coordinate, which detectCity rejects
            // with the same exception type. A narrower catch would turn one bad row into a 500.
            return false;
        }
    }

    private Location toLocation(LocationIqResult result) {
        LocationIqAddress address = result.getAddress();

        Location.LocationBuilder builder = Location.builder()
                .formattedAddress(result.getDisplayName())
                .osmId(result.getOsmId());

        if (address != null) {
            builder.country(address.getCountry())
                    .city(address.getCity())
                    .district(firstNonNull(address.getSuburb(), address.getCityDistrict(), address.getNeighbourhood()))
                    .street(firstNonNull(address.getRoad(), address.getName()));
        }

        return builder.build();
    }

    private Double parseCoordinate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private AutoCompleteSuggestionDTO toAutoCompleteDTO(LocationIqResult result) {
        AutoCompleteSuggestionDTO dto = new AutoCompleteSuggestionDTO();
        dto.setOsmId(result.getOsmId());
        dto.setOsmType(result.getOsmType());
        dto.setDisplayName(result.getDisplayName());
        dto.setDisplayPlace(result.getDisplayPlace() != null
                ? result.getDisplayPlace()
                : shortPlace(result.getAddress()));
        dto.setDisplayAddress(result.getDisplayAddress() != null
                ? result.getDisplayAddress()
                : shortArea(result.getAddress()));
        dto.setLatitude(parseCoordinate(result.getLat()));
        dto.setLongitude(parseCoordinate(result.getLon()));
        return dto;
    }

    private String addressLanguage() {
        return "sr".equals(LocaleContextHolder.getLocale().getLanguage()) ? "sr-Latn" : "en,sr-Latn";
    }

    private String shortPlace(LocationIqAddress address) {
        if (address == null) {
            return null;
        }
        if (address.getRoad() != null) {
            return address.getHouseNumber() != null
                    ? address.getRoad() + " " + address.getHouseNumber()
                    : address.getRoad();
        }
        return address.getName();
    }

    private String shortArea(LocationIqAddress address) {
        if (address == null) {
            return null;
        }
        String district = firstNonNull(address.getSuburb(), address.getCityDistrict(), address.getNeighbourhood());
        if (district == null || district.equals(address.getCity())) {
            return address.getCity();
        }
        return address.getCity() != null ? district + ", " + address.getCity() : district;
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
