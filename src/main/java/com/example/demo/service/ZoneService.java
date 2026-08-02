package com.example.demo.service;

import com.example.demo.dto.CityZoneDto;
import com.example.demo.dto.ReportZoneDto;
import com.example.demo.model.City;
import com.example.demo.model.Zone;
import com.example.demo.repository.ZoneRepository;
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

    /**
     * Najdublja zona koja sadrzi datu tacku — mesna zajednica ili naseljeno mesto ako
     * postoji, inace jedinica nivoa 1 — ili prazno ako tacka nije ni u jednoj poznatoj zoni,
     * odnosno ni u jednom gradu koji aplikacija pokriva.
     */
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
     * Zona sa granicom u GeoJSON obliku, za prikaz na mapi detalja oglasa.
     * Prima id (ne entitet) da citanje ne inicijalizuje LAZY Zone proxy — naziv i
     * grad dolaze istim upitom kao geometrija.
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
     * Grubi nivo grada koji korisnik pretrazuje: 17 beogradskih opstina, ali samo po jedna
     * jedinica u Novom Sadu i Bajinoj Basti, gde nivo 1 pokriva ceo grad.
     *
     * Jedna stavka je klijentu znak da kontrolu ne prikaze — a ne da je grad poseban. Time
     * na klijentu nema nijedne provere po gradu; razlika izmedju Beograda i ostalih ispada
     * iz duzine ove liste.
     */
    @Transactional(readOnly = true)
    public List<CityZoneDto> getAreas(String userEmail) {
        City city = cityService.getActiveCity(userEmail);
        return zoneRepository.findByCityIdAndLevelOrderByNameAsc(city.getId(), AREA_LEVEL).stream()
                .map(ZoneService::toDto)
                .toList();
    }

    /**
     * Fini nivo grada koji korisnik pretrazuje, opciono sazet na jednu oblast.
     *
     * {@code areaId} je namerno opcion: meni radi i kad gruba oblast nije izabrana, pa se
     * moze pretrazivati kroz ceo grad odjednom i tek onda suziti. Izostavljen {@code areaId}
     * je i jedini oblik koji Novi Sad i Bajina Basta koriste.
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
