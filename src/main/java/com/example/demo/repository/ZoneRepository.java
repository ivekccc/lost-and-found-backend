package com.example.demo.repository;

import com.example.demo.model.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    List<Zone> findByCityIdAndLevelOrderByNameAsc(Long cityId, short level);

    /**
     * Zone jednog nivoa u jednom gradu, opciono sazete na decu jedne oblasti i opciono
     * pretrazene po imenu.
     *
     * Pretraga ide kroz {@code unaccent} sa OBE strane. Bez toga bi ovaj meni imao losiju
     * pretragu od postojeceg {@code Picker}-a, koji trazi lokalno i presavija dijakritiku —
     * „zarkovo" ne bi naslo „Zarkovo". Presavijanje radi iskljucivo baza; da se pojam
     * presavija u Javi, dve implementacije bi se razisle bas na „đ", koje nije slovo sa
     * akcentom nego zaseban znak i {@code java.text.Normalizer} ga ne dira.
     *
     * Izostavljena pretraga stize kao {@code %}, a ne kao null: Hibernate ne ume da otipizuje
     * null parametar unutar {@code FUNCTION('unaccent', :search)}, pa bi grana
     * {@code :search IS NULL} rusila upit umesto da ga preskoci.
     */
    @Query("SELECT z FROM Zone z WHERE z.cityId = :cityId AND z.level = :level "
            + "AND (:parentId IS NULL OR z.parentId = :parentId) "
            + "AND LOWER(FUNCTION('unaccent', z.name)) LIKE LOWER(FUNCTION('unaccent', :search)) "
            + "ORDER BY z.name")
    Page<Zone> findFilterZones(@Param("cityId") Long cityId,
                               @Param("level") short level,
                               @Param("parentId") Long parentId,
                               @Param("search") String search,
                               Pageable pageable);

    /**
     * Najdublja zona koja geometrijski sadrzi datu tacku: mesna zajednica ako postoji,
     * inace opstina. Redosled argumenata je (longitude, latitude) jer ST_MakePoint prima (x, y).
     *
     * Celo pravilo zivi u SQL funkciji {@code zone_resolve} (migracija V40), a ne ovde,
     * zato sto ga pored aplikacije mora izvrsavati i backfill u migracijama. Ranije je
     * postojalo na dva mesta koja su se vec razisla: V34 je koristio ST_Contains bez
     * fallback-a, a ovaj upit ST_Covers sa fallback-om.
     *
     * Funkcija je set-returning, pa nerazresena tacka daje NULA redova i mapira se u
     * {@code Optional.empty()}; skalarna varijanta bi vratila jedan red sa null vrednoscu.
     */
    @Query(value = "SELECT zone_id FROM zone_resolve(:latitude, :longitude)", nativeQuery = true)
    Optional<Long> findZoneIdContaining(@Param("latitude") double latitude,
                                        @Param("longitude") double longitude);

    /**
     * Zona sa granicom kao GeoJSON geometrija za prikaz na mapi, uz naziv roditelja
     * (null za nivo 1 i za naselja koja se zovu isto kao opstina — vidi V39). Sve ide
     * jednim upitom da se izbegne inicijalizacija LAZY Zone proxy-ja; parent_name je
     * denormalizovana kolona, pa nije potreban ni JOIN.
     *
     * Uproscavanje za prikaz ide po POVRSINI, ne po nivou: bitno je koliko je zona krupna
     * na ekranu, a ne kojoj administrativnoj kategoriji pripada. Seosko naselje nivoa 2
     * ume da bude vece od gradske opstine (Padinska Skela 216 km2 naspram Vracara 2.9).
     *
     * Iznad 2 km2 ide 0.0005 stepeni (~55 m), sto je ispod 4% sirine takve zone i na
     * kartici visine 180 pt se ne vidi. Ispod 2 km2 salje se geometrija kakva jeste
     * (u bazi je vec na ~11 m): tu bi 55 m bilo 6% sirine i vidno bi deformisalo oblik,
     * a najsitnije jedinice srusilo u trougao. Efekat: prosecan payload 1.3 KB umesto
     * 5 KB, najveci 12.8 KB umesto 20 KB, uz 77 najsitnijih zona netaknutih.
     *
     * ST_Multi stoji IZVAN CASE-a: ST_SimplifyPreserveTopology raspakuje jednodelni
     * MultiPolygon u Polygon, pa bi bez njega tip geometrije zavisio od podataka. Time
     * nijedna grana ne moze da propusti goli Polygon i klijent ima jednu putanju parsiranja.
     */
    @Query(value = "SELECT z.name AS name, z.city AS city, z.parent_name AS \"parentName\", "
            + "ST_AsGeoJSON(ST_Multi("
            + "  CASE WHEN z.area_km2 > 2 "
            + "       THEN ST_SimplifyPreserveTopology(z.boundary, 0.0005) "
            + "       ELSE z.boundary "
            + "  END), 5) AS \"boundaryGeoJson\" "
            + "FROM zones z WHERE z.id = :zoneId", nativeQuery = true)
    Optional<ZoneBoundaryView> findZoneBoundary(@Param("zoneId") Long zoneId);
}
