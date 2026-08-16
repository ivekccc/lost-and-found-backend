package com.example.demo.location.service;

import com.example.demo.location.dto.AutoCompleteSuggestionDTO;
import com.example.demo.location.dto.CityDto;
import com.example.demo.location.dto.ReverseGeocodeDto;
import com.example.demo.location.dto.locationiq.LocationIqAddress;
import com.example.demo.location.dto.locationiq.LocationIqResult;
import com.example.demo.location.model.City;
import com.example.demo.location.model.Location;
import com.example.demo.shared.exception.LocationOutsideSupportedCitiesException;
import com.example.demo.shared.exception.RateLimitExceededException;
import com.example.demo.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
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
                + "&limit=" + CANDIDATE_LIMIT;
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

    /**
     * The address under a pair of device coordinates.
     *
     * The city is decided from the coordinates of the MATCHED ADDRESS, not from the device's own
     * position: the listing later resolves its zone through {@link #lookupLocation}, which reads
     * the address, so judging the device point could accept a fix near a city border whose nearest
     * address lies outside it — and the listing would then land in the wrong city or none at all.
     */
    public ReverseGeocodeDto reverseGeocode(double latitude, double longitude, String userEmail) {
        if (!isValidCoordinate(latitude, 90) || !isValidCoordinate(longitude, 180)) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90, longitude between -180 and 180");
        }

        City activeCity = cityService.getActiveCity(userEmail);
        String url = baseUrl + "/reverse"
                + "?key=" + apiKey
                + "&lat=" + latitude
                + "&lon=" + longitude
                + "&format=json"
                + "&addressdetails=1";

        LocationIqResult result;
        try {
            // Unlike /autocomplete and /lookup, /reverse answers with a single object.
            result = restTemplate.getForObject(url, LocationIqResult.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("No address found at these coordinates");
        } catch (HttpClientErrorException.TooManyRequests e) {
            // Autocomplete degrades to an empty list here, but an empty answer means nothing to a
            // button the user just pressed: without this the provider's throttling surfaced as a
            // bare 500, and the fix is simply to wait a moment.
            throw new RateLimitExceededException(
                    "Location service is busy right now — try again in a moment",
                    LOCATIONIQ_RETRY_AFTER_SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("LocationIQ API error: " + e.getMessage(), e);
        }

        if (result == null || result.getLat() == null || result.getLon() == null) {
            throw new ResourceNotFoundException("No address found at these coordinates");
        }

        double resultLatitude;
        double resultLongitude;
        try {
            resultLatitude = Double.parseDouble(result.getLat());
            resultLongitude = Double.parseDouble(result.getLon());
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("No usable address found at these coordinates");
        }

        CityDto city = cityService
                .detectCity(resultLatitude, resultLongitude)
                .orElseThrow(() -> new LocationOutsideSupportedCitiesException(
                        "This place is outside the cities the app covers — Beograd, Novi Sad and Bajina Bašta"));

        return new ReverseGeocodeDto(
                toAutoCompleteDTO(result),
                city,
                city.getId().equals(activeCity.getId()));
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

    public Location lookupLocation(String osmId, String osmType) {
        String typePrefix = osmType.substring(0, 1).toUpperCase();
        String url = "https://us1.locationiq.com/v1/lookup"
                + "?key=" + apiKey
                + "&osm_ids=" + typePrefix + osmId
                + "&format=json"
                + "&addressdetails=1";

        LocationIqResult[] results = restTemplate.getForObject(url, LocationIqResult[].class);
        if (results == null || results.length == 0) {
            throw new RuntimeException("Location not found for OSM ID: " + osmId);
        }

        LocationIqResult result = results[0];
        LocationIqAddress address = result.getAddress();

        Location.LocationBuilder builder = Location.builder()
                .latitude(new BigDecimal(result.getLat()))
                .longitude(new BigDecimal(result.getLon()))
                .formattedAddress(result.getDisplayName())
                .osmId(osmId);

        if (address != null) {
            builder.country(address.getCountry())
                    .city(address.getCity())
                    .district(firstNonNull(address.getSuburb(), address.getCityDistrict(), address.getNeighbourhood()))
                    .street(firstNonNull(address.getRoad(), address.getName()));
        }

        return builder.build();
    }

    private AutoCompleteSuggestionDTO toAutoCompleteDTO(LocationIqResult result) {
        AutoCompleteSuggestionDTO dto = new AutoCompleteSuggestionDTO();
        dto.setOsmId(result.getOsmId());
        dto.setOsmType(result.getOsmType());
        dto.setDisplayName(result.getDisplayName());
        dto.setDisplayPlace(result.getDisplayPlace());
        dto.setDisplayAddress(result.getDisplayAddress());
        return dto;
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
