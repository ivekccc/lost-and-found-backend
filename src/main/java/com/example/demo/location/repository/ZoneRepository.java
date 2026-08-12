package com.example.demo.location.repository;

import com.example.demo.location.model.Zone;

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
     * Zones of one level in one city, optionally narrowed to the children of an area and searched
     * by name. The search folds diacritics through unaccent on both sides, so "zarkovo" finds
     * "Žarkovo". An omitted search arrives as %, not null: Hibernate cannot type a null inside a
     * function call, and the null branch would break the whole query.
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
     * The deepest zone geometrically containing the point. Argument order is (longitude, latitude)
     * because ST_MakePoint takes (x, y).
     *
     * The rule lives in the zone_resolve SQL function so migrations and the application share it.
     * Set-returning, so an unresolved point yields zero rows and maps to Optional.empty().
     */
    @Query(value = "SELECT zone_id FROM zone_resolve(:latitude, :longitude)", nativeQuery = true)
    Optional<Long> findZoneIdContaining(@Param("latitude") double latitude,
                                        @Param("longitude") double longitude);

    /**
     * Zone boundary as GeoJSON for the map on a listing, with the parent name alongside.
     *
     * Simplification is chosen by AREA, not by level: a rural settlement can be larger than a city
     * municipality. ST_Multi sits OUTSIDE the CASE because ST_SimplifyPreserveTopology unpacks a
     * single-part MultiPolygon into a Polygon, which would make the geometry type depend on data.
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
