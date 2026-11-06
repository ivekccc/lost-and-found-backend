package com.example.demo.report.repository;

import com.example.demo.report.dto.MatchCandidateView;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    List<Report> findByUserId(Long userId);

    Optional<Report> findByShareToken(UUID shareToken);

    /**
     * Listing loaded with an exclusive row lock. Every decision that changes claim state on one
     * listing must serialise, or two transactions each read their claim as PENDING and one reveals
     * contact details to someone whose claim ends up DECLINED.
     *
     * The lock must be taken BEFORE the claim is loaded — otherwise Hibernate returns a cached
     * instance and the check still reads stale state after waiting.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Report r WHERE r.id = :id")
    Optional<Report> findByIdForUpdate(@Param("id") Long id);

    List<Report> findByStatusAndExpiresAtBefore(ReportStatus status, LocalDateTime expiresAt);

    List<Report> findByStatusAndType(ReportStatus status, ReportType type);

    /**
     * Matching candidates: opposite type, same category, someone else's listing, SAME CITY. The city
     * condition is explicit even though the 25 km limit mostly implies it — Bajina Bašta sits closer
     * than that to neighbouring municipalities.
     *
     * Text is folded through unaccent before comparison, or "novcanik" and "novčanik" are
     * different trigram sets and two listings about the same thing lose part of their score.
     */
    @Query(value = "SELECT r.id AS id, r.created_at AS createdAt, "
            + "l.latitude AS latitude, l.longitude AS longitude, "
            + "similarity(unaccent(lower(:probeText)), "
            + "           unaccent(lower(r.title || ' ' || COALESCE(r.description, '')))) AS similarity "
            + "FROM reports r "
            + "JOIN locations l ON l.id = r.location_id "
            + "JOIN zones z ON z.id = l.zone_id "
            + "WHERE r.type = :type AND r.status = 'ACTIVE' "
            + "AND r.category_id = :categoryId AND r.user_id <> :ownerId "
            + "AND z.city_id = :cityId",
            nativeQuery = true)
    List<MatchCandidateView> findMatchCandidates(@Param("type") String type,
                                                 @Param("categoryId") Long categoryId,
                                                 @Param("ownerId") Long ownerId,
                                                 @Param("probeText") String probeText,
                                                 @Param("cityId") Long cityId);

    /**
     * What one city has done since the application started, both counts in a single pass.
     *
     * Reunions are read from resolved_at, NOT from status = 'RESOLVED': moderation overwrites the
     * status, so a flag/unflag cycle would quietly undo the reunion.
     */
    @Query(value = "SELECT COUNT(*) AS reportsPosted, "
            + "COUNT(*) FILTER (WHERE r.resolved_at IS NOT NULL) AS reportsReunited "
            + "FROM reports r "
            + "JOIN locations l ON l.id = r.location_id "
            + "JOIN zones z ON z.id = l.zone_id "
            + "WHERE z.city_id = :cityId "
            + "AND r.status NOT IN ('DELETED', 'FLAGGED')",
            nativeQuery = true)
    CommunityStatisticsView findCommunityStatistics(@Param("cityId") Long cityId);
}
