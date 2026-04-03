package com.example.demo.service;

import com.example.demo.dto.AutoCompleteSuggestionDTO;
import com.example.demo.dto.locationiq.LocationIqAddress;
import com.example.demo.dto.locationiq.LocationIqResult;
import com.example.demo.model.Location;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {
    @Value("${locationiq.api-key}")
    private String apiKey;

    @Value("${locationiq.base-url}")
    private String baseUrl;

    @Value("${locationiq.default-country}")
    private String defaultCountry;

    private final RestTemplate restTemplate;

    public List<AutoCompleteSuggestionDTO> getAutoCompleteSuggestions(String query) {
        String url = baseUrl + "/autocomplete"
                + "?key=" + apiKey
                + "&q=" + query
                + "&countrycodes=" + defaultCountry
                + "&viewbox=20.22,44.93,20.65,44.68"
                + "&limit=5";
        try {
            LocationIqResult[] results = restTemplate.getForObject(url, LocationIqResult[].class);
            if (results == null || results.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.stream(results)
                    .map(this::toAutoCompleteDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("LocationIQ API error: " + e.getMessage(), e);
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
