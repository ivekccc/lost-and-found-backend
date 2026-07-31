package com.example.demo.service;

import com.example.demo.dto.ReportZoneDto;
import com.example.demo.model.Zone;
import com.example.demo.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;

    /**
     * Najdublja zona koja sadrzi datu tacku — mesna zajednica ili naseljeno mesto ako
     * postoji, inace gradska opstina — ili prazno ako tacka nije ni u jednoj poznatoj
     * zoni (npr. lokacija van Beograda).
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
}
