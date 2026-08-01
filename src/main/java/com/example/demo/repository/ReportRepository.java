package com.example.demo.repository;

import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    List<Report> findByUserId(Long userId);

    /**
     * Oglas ucitan sa ekskluzivnim zakljucavanjem reda (SELECT ... FOR UPDATE).
     *
     * Sve odluke koje menjaju stanje claim-ova na jednom oglasu — odobravanje, odbijanje i
     * zatvaranje oglasa — moraju da se serijalizuju, jer se odnose na isti resurs iako
     * dodiruju razlicite claim redove. Bez toga dve paralelne transakcije obe procitaju svoj
     * claim kao PENDING iz sopstvenog snapshot-a, obe prodju provere, i jedna vrati kontakt
     * podnosiocu ciji claim na kraju ostane DECLINED.
     *
     * VAZNO: zakljucavanje mora da se izvrsi PRE ucitavanja claim-a. Ako je claim vec ucitan
     * u persistence context, Hibernate vraca kesiranu instancu i druga transakcija bi i posle
     * cekanja videla zastarelo PENDING stanje — zakljucavanje bi izgledalo ispravno, a ne bi
     * radilo. Zato se id oglasa dobija projekcijom (ClaimRepository.findReportIdByClaimId),
     * bez ucitavanja entiteta.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Report r WHERE r.id = :id")
    Optional<Report> findByIdForUpdate(@Param("id") Long id);

    List<Report> findByStatusAndExpiresAtBefore(ReportStatus status, LocalDateTime expiresAt);

    List<Report> findByStatusAndType(ReportStatus status, ReportType type);

    /**
     * Kandidati za mecovanje: suprotan tip, ista kategorija, tudji oglas, ISTI GRAD.
     *
     * Uslov po gradu je eksplicitan iako ga granica od 25 km uglavnom vec postuje. "Uglavnom"
     * nije dovoljno: Bajina Basta je od susednih opstina bliza od 25 km, pa bi bez ovoga
     * korisnik dobio mec sa oglasom iz podrucja koje ni ne pretrazuje i ne moze da otvori.
     * Grad se cita iz zone lokacije, sto je isti izvor po kome se filtrira i pretraga.
     */
    @Query(value = "SELECT r.id AS id, r.created_at AS createdAt, "
            + "l.latitude AS latitude, l.longitude AS longitude, "
            + "similarity(lower(:probeText), lower(r.title || ' ' || COALESCE(r.description, ''))) AS similarity "
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
     * Sta je jedan grad uradio otkako aplikacija postoji, oba broja jednim prolazom.
     *
     * Spojenost se cita iz {@code resolved_at IS NOT NULL}, a NE iz {@code status = 'RESOLVED'} —
     * moderacija prepisuje status (AdminReportService.restoredStatusFor), pa bi ciklus flag/unflag
     * tiho ponistio spajanje. Grad ide kroz zonu lokacije, isti izvor po kome se filtriraju
     * pretraga, nearby i kandidati za mecovanje.
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
