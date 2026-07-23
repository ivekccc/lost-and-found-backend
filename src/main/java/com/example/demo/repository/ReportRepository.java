package com.example.demo.repository;

import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    List<Report> findByUserId(Long userId);

    List<Report> findByStatusAndExpiresAtBefore(ReportStatus status, LocalDateTime expiresAt);

    List<Report> findByStatusAndType(ReportStatus status, ReportType type);

    @Query(value = "SELECT r.id AS id, r.created_at AS createdAt, "
            + "l.latitude AS latitude, l.longitude AS longitude, "
            + "similarity(lower(:probeText), lower(r.title || ' ' || COALESCE(r.description, ''))) AS similarity "
            + "FROM reports r "
            + "JOIN locations l ON l.id = r.location_id "
            + "WHERE r.type = :type AND r.status = 'ACTIVE' "
            + "AND r.category_id = :categoryId AND r.user_id <> :ownerId",
            nativeQuery = true)
    List<MatchCandidateView> findMatchCandidates(@Param("type") String type,
                                                 @Param("categoryId") Long categoryId,
                                                 @Param("ownerId") Long ownerId,
                                                 @Param("probeText") String probeText);
}
