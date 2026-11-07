package com.example.demo.report.repository;

import com.example.demo.report.model.AbuseReport;
import com.example.demo.report.model.AbuseReportStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
public interface AbuseReportRepository extends JpaRepository<AbuseReport, Long>, JpaSpecificationExecutor<AbuseReport> {

    long countByStatus(AbuseReportStatus status);

    @Override
    @EntityGraph(attributePaths = {"reporter", "targetUser", "targetReport", "reviewedBy"})
    Page<AbuseReport> findAll(Specification<AbuseReport> specification, Pageable pageable);

    @Query("SELECT MIN(a.createdAt) FROM AbuseReport a WHERE a.status = :status")
    LocalDateTime findOldestCreatedAt(@Param("status") AbuseReportStatus status);

    long countByTargetReportIdAndStatus(Long targetReportId, AbuseReportStatus status);

    @Query("SELECT a.targetReport.id FROM AbuseReport a "
            + "WHERE a.targetReport.id IN :reportIds AND a.status = :status "
            + "GROUP BY a.targetReport.id HAVING COUNT(a) >= :threshold")
    List<Long> findReportIdsWithAtLeast(@Param("reportIds") List<Long> reportIds,
                                        @Param("status") AbuseReportStatus status,
                                        @Param("threshold") long threshold);

    long countByReporterIdAndCreatedAtAfter(Long reporterId, LocalDateTime after);

    boolean existsByReporterIdAndTargetUserIdAndStatus(Long reporterId, Long targetUserId, AbuseReportStatus status);

    boolean existsByReporterIdAndTargetReportIdAndStatus(Long reporterId, Long targetReportId, AbuseReportStatus status);

    boolean existsByReporterIdAndTargetMessageIdAndStatus(Long reporterId, Long targetMessageId, AbuseReportStatus status);

    List<AbuseReport> findByTargetUserIdAndStatus(Long targetUserId, AbuseReportStatus status);

    List<AbuseReport> findByTargetReportIdAndStatus(Long targetReportId, AbuseReportStatus status);
}
