package com.example.demo.service;

import com.example.demo.dto.AutoCompleteSuggestionDTO;
import com.example.demo.dto.locationiq.LocationIqAddress;
import com.example.demo.dto.locationiq.LocationIqResult;
import com.example.demo.model.City;
import com.example.demo.model.Location;
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

    // Trazi se vise kandidata nego sto se prikazuje, jer filter po zoni odbacuje deo njih.
    // Bez toga bi pretraga u prigradskim delovima cesto vracala jedan predlog ili nijedan.
    private static final int CANDIDATE_LIMIT = 15;
    private static final int SUGGESTION_LIMIT = 5;

    @Value("${locationiq.api-key}")
    private String apiKey;

    @Value("${locationiq.base-url}")
    private String baseUrl;

    @Value("${locationiq.default-country}")
    private String defaultCountry;

    private final RestTemplate restTemplate;
    private final CityService cityService;

    /**
     * Predlozi adresa, ograniceni na grad koji korisnik trenutno pretrazuje.
     *
     * Okvir je unija granica zona nivoa 1 tog grada (racunata u V47), a ne rucno upisan
     * pravougaonik: zatecena konstanta 20.22,44.93,20.65,44.68 pokrivala je samo urbano
     * jezgro Beograda, pa adrese u Obrenovcu, Lazarevcu, Mladenovcu i Sopotu — preko polovine
     * povrsine grada — pretraga nikada nije nalazila. Izvodenjem iz istih poligona koji cine
     * zone nemoguce je da se okvir i zone raziđu, pa nema ni adrese koja se moze izabrati a
     * posle nema zonu.
     *
     * LocationIQ trazi viewbox u redosledu min_lon,max_lat,max_lon,min_lat.
     *
     * Okvir je i dalje PRAVOUGAONIK, a gradovi to nisu: okvir Beograda preko Dunava zahvata
     * Pancevo, Staru Pazovu i Rumu. Zato se predlozi posle dohvatanja propustaju kroz zone —
     * inace korisnik dobije adresu koju izabere, popuni celu formu, pa ga createReport odbije
     * sa 400. To je obrazac „dugme koje vodi u 400", isti bag kao da zastite nema.
     *
     * Provera ide kroz {@code CityService.detectCity}, koji unutra zove istu {@code zone_resolve}
     * funkciju kojom se oglasu dodeljuje zona. To je nosivi deo: filter i kasniji gard ne mogu
     * da se raziđu, ukljucujuci i toleranciju tira 3 (~333 m van granice grada) — adresa koju
     * bi server prihvatio prolazi i kroz filter.
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
            // Stream je lenj, pa se zona razresava samo dok se ne skupi SUGGESTION_LIMIT
            // predloga — u praksi 5-6 upita, ne svih CANDIDATE_LIMIT.
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
            // Pokriva i neparsivu vrednost i koordinatu van opsega, koju detectCity odbija istim
            // tipom izuzetka. Bez ovoga bi neispravan red iz LocationIQ-a prosao do catch-all
            // bloka iznad i ceo upit vratio 500 umesto da se taj jedan predlog preskoci.
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
