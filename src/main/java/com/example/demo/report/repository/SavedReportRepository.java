package com.example.demo.report.repository;

import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.SavedReport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface SavedReportRepository extends JpaRepository<SavedReport, Long> {

    boolean existsByUserIdAndReportId(Long userId, Long reportId);

    /** @Modifying @Query rather than a derived deleteBy...: the derived form loads entities first. */
    @Modifying
    @Query("DELETE FROM SavedReport s WHERE s.user.id = :userId AND s.report.id = :reportId")
    void deleteByUserIdAndReportId(@Param("userId") Long userId,
                                   @Param("reportId") Long reportId);

    /**
     * A user's saved listings, newest first. JOIN FETCH of location and zone because the location
     * label is built for every row, and deleted or moderated listings drop out — opening one 404s.
     */
    @Modifying
    @Query("DELETE FROM SavedReport s WHERE s.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT s.report FROM SavedReport s "
            + "LEFT JOIN FETCH s.report.location l "
            + "LEFT JOIN FETCH l.zone "
            + "WHERE s.user.id = :userId "
            + "AND s.report.status NOT IN (com.example.demo.report.model.ReportStatus.DELETED, "
            + "                            com.example.demo.report.model.ReportStatus.FLAGGED) "
            + "ORDER BY s.createdAt DESC")
    List<Report> findSavedReports(@Param("userId") Long userId);
}
