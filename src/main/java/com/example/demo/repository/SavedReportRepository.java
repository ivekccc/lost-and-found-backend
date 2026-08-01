package com.example.demo.repository;

import com.example.demo.model.Report;
import com.example.demo.model.SavedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SavedReportRepository extends JpaRepository<SavedReport, Long> {

    boolean existsByUserIdAndReportId(Long userId, Long reportId);

    /**
     * Brisanje ide kroz {@code @Modifying @Query}, ne kroz izvedeni {@code deleteBy...}:
     * izvedeni oblik prvo ucita entitet pa ga obrise, a {@code @Modifying} nad njim nema
     * dejstva i samo obmanjuje citaoca. Bez {@code clearAutomatically} — brisanje ovog reda
     * ne utice ni na jedan drugi entitet u kontekstu, pa nema sta da se odbaci.
     */
    @Modifying
    @Query("DELETE FROM SavedReport s WHERE s.user.id = :userId AND s.report.id = :reportId")
    void deleteByUserIdAndReportId(@Param("userId") Long userId,
                                   @Param("reportId") Long reportId);

    /**
     * Sacuvani oglasi korisnika, najnoviji prvo.
     *
     * {@code JOIN FETCH} lokacije i zone: labela lokacije se gradi za SVAKI red spiska
     * ({@code LocationDTO.zonalFromEntity}), pa bi bez toga svaka kartica povukla svoj upit.
     * Isti razlog kao {@code ReportSpecifications.withLocationZone}.
     *
     * Status se filtrira ovde, a ne oslanjanjem na {@code ON DELETE CASCADE}: brisanje oglasa
     * je MEKO ({@code deleteReport} postavlja status, ne brise red), pa kaskada nikad ne
     * okine. Bez ovog uslova bi obrisan ili sakriven oglas ostao u spisku i vodio na 404.
     */
    @Query("SELECT s.report FROM SavedReport s "
            + "LEFT JOIN FETCH s.report.location l "
            + "LEFT JOIN FETCH l.zone "
            + "WHERE s.user.id = :userId "
            + "AND s.report.status NOT IN (com.example.demo.model.ReportStatus.DELETED, "
            + "                            com.example.demo.model.ReportStatus.FLAGGED) "
            + "ORDER BY s.createdAt DESC")
    List<Report> findSavedReports(@Param("userId") Long userId);
}
