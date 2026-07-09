package com.example.demo.repository;

import com.example.demo.model.AbuseReport;
import com.example.demo.model.AbuseReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AbuseReportRepository extends JpaRepository<AbuseReport, Long> {

    long countByTargetReportIdAndStatus(Long targetReportId, AbuseReportStatus status);

    @Query("SELECT a.targetReport.id FROM AbuseReport a "
            + "WHERE a.targetReport.id IN :reportIds AND a.status = :status "
            + "GROUP BY a.targetReport.id HAVING COUNT(a) >= :threshold")
    List<Long> findReportIdsWithAtLeast(@Param("reportIds") List<Long> reportIds,
                                        @Param("status") AbuseReportStatus status,
                                        @Param("threshold") long threshold);


    List<AbuseReport> findByStatusOrderByCreatedAtDesc(AbuseReportStatus status);

    List<AbuseReport> findAllByOrderByCreatedAtDesc();

    long countByReporterIdAndCreatedAtAfter(Long reporterId, LocalDateTime after);

    boolean existsByReporterIdAndTargetUserIdAndStatus(Long reporterId, Long targetUserId, AbuseReportStatus status);

    boolean existsByReporterIdAndTargetReportIdAndStatus(Long reporterId, Long targetReportId, AbuseReportStatus status);

    List<AbuseReport> findByTargetUserIdAndStatus(Long targetUserId, AbuseReportStatus status);

    List<AbuseReport> findByTargetReportIdAndStatus(Long targetReportId, AbuseReportStatus status);
}
