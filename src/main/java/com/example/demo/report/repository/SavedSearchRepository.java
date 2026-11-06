package com.example.demo.report.repository;

import com.example.demo.report.model.SavedSearch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

    @Query("SELECT s FROM SavedSearch s "
            + "LEFT JOIN FETCH s.category "
            + "LEFT JOIN FETCH s.zone "
            + "JOIN FETCH s.city "
            + "WHERE s.user.id = :userId "
            + "ORDER BY s.createdAt DESC")
    List<SavedSearch> findAllOfUser(@Param("userId") Long userId);

    long countByUserId(Long userId);

    Optional<SavedSearch> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM SavedSearch s WHERE s.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT DISTINCT s.user_id "
            + "FROM saved_searches s "
            + "JOIN reports r ON r.id = :reportId "
            + "JOIN locations l ON l.id = r.location_id "
            + "JOIN zones z ON z.id = l.zone_id "
            + "JOIN users u ON u.id = s.user_id "
            + "WHERE s.city_id = z.city_id "
            + "AND s.user_id <> r.user_id "
            + "AND u.status IN ('ACTIVE', 'PARTIALLY_BLOCKED') "
            + "AND r.status = 'ACTIVE' "
            + "AND (s.type IS NULL OR s.type = r.type) "
            + "AND (s.category_id IS NULL OR s.category_id = r.category_id) "
            + "AND (s.zone_id IS NULL OR s.zone_id = z.id OR s.zone_id = z.parent_id) "
            + "AND (s.search_text IS NULL "
            + "     OR strpos(unaccent(lower(r.title)), unaccent(lower(s.search_text))) > 0 "
            + "     OR strpos(unaccent(lower(COALESCE(r.description, ''))), "
            + "               unaccent(lower(s.search_text))) > 0)",
            nativeQuery = true)
    List<Long> findSubscribersOf(@Param("reportId") Long reportId);
}
