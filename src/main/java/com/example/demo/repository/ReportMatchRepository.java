package com.example.demo.repository;

import com.example.demo.model.ReportMatch;
import com.example.demo.model.ReportMatchStatus;
import com.example.demo.model.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportMatchRepository extends JpaRepository<ReportMatch, Long>, JpaSpecificationExecutor<ReportMatch> {

    Optional<ReportMatch> findByLostReportIdAndFoundReportId(Long lostReportId, Long foundReportId);

    @Query("SELECT m FROM ReportMatch m "
            + "WHERE m.status = :status "
            + "AND m.lostReport.status = :reportStatus AND m.foundReport.status = :reportStatus "
            + "AND ((m.lostReport.id = :reportId AND m.lostDismissedAt IS NULL) "
            + "  OR (m.foundReport.id = :reportId AND m.foundDismissedAt IS NULL)) "
            + "ORDER BY m.score DESC")
    List<ReportMatch> findVisibleByReportId(@Param("reportId") Long reportId,
                                            @Param("status") ReportMatchStatus status,
                                            @Param("reportStatus") ReportStatus reportStatus);

    @Query("SELECT m FROM ReportMatch m "
            + "WHERE m.status = :status "
            + "AND m.lostReport.status = :reportStatus AND m.foundReport.status = :reportStatus "
            + "AND ((m.lostReport.user.id = :userId AND m.lostDismissedAt IS NULL) "
            + "  OR (m.foundReport.user.id = :userId AND m.foundDismissedAt IS NULL)) "
            + "ORDER BY m.score DESC")
    List<ReportMatch> findVisibleByUserId(@Param("userId") Long userId,
                                          @Param("status") ReportMatchStatus status,
                                          @Param("reportStatus") ReportStatus reportStatus,
                                          Pageable pageable);

    @Query("SELECT m.lostReport.id, COUNT(m) FROM ReportMatch m "
            + "WHERE m.lostReport.id IN :reportIds AND m.status = :status "
            + "AND m.lostDismissedAt IS NULL "
            + "AND m.lostReport.status = :reportStatus AND m.foundReport.status = :reportStatus "
            + "GROUP BY m.lostReport.id")
    List<Object[]> countVisibleByLostReportIds(@Param("reportIds") List<Long> reportIds,
                                               @Param("status") ReportMatchStatus status,
                                               @Param("reportStatus") ReportStatus reportStatus);

    @Query("SELECT m.foundReport.id, COUNT(m) FROM ReportMatch m "
            + "WHERE m.foundReport.id IN :reportIds AND m.status = :status "
            + "AND m.foundDismissedAt IS NULL "
            + "AND m.lostReport.status = :reportStatus AND m.foundReport.status = :reportStatus "
            + "GROUP BY m.foundReport.id")
    List<Object[]> countVisibleByFoundReportIds(@Param("reportIds") List<Long> reportIds,
                                                @Param("status") ReportMatchStatus status,
                                                @Param("reportStatus") ReportStatus reportStatus);
}
