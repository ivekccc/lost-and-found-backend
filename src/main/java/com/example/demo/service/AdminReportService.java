package com.example.demo.service;

import com.example.demo.dto.AdminReportDetailsDTO;
import com.example.demo.dto.AdminReportListDto;
import com.example.demo.dto.LocationDTO;
import com.example.demo.dto.ReportImageDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.ClaimStatus;
import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import com.example.demo.model.User;
import com.example.demo.repository.ClaimRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.ReportSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final ClaimRepository claimRepository;
    private final AbuseReportService abuseReportService;

    /**
     * Sve oglase za moderaciju: svi statusi osim DELETED, svi vlasnici, uvek tacna
     * lokacija. Backoffice namerno NE koristi javni GET /reports — on filtrira samo
     * ACTIVE i izbacuje oglase samog pozivaoca, pa FLAGGED oglasi tamo nisu vidljivi.
     */
    @Transactional(readOnly = true)
    public List<AdminReportListDto> getReports(ReportType type, ReportStatus status, Long cityId) {
        Specification<Report> spec = Specification.allOf(
                ReportSpecifications.statusNot(ReportStatus.DELETED),
                ReportSpecifications.hasType(type),
                ReportSpecifications.hasStatus(status),
                ReportSpecifications.inCity(cityId),
                ReportSpecifications.withLocationZone()
        );

        return reportRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::mapToListDto)
                .toList();
    }

    private AdminReportListDto mapToListDto(Report report) {
        return new AdminReportListDto(
                report.getId(),
                report.getTitle(),
                report.getType(),
                report.getCategory().getName(),
                report.getStatus(),
                LocationDTO.fromEntity(report.getLocation()),
                report.getCreatedAt(),
                report.getUser().getId(),
                buildFullName(report.getUser()),
                cityNameOf(report)
        );
    }

    /**
     * Ime grada iz zone lokacije. Prazno je smislen odgovor, ne greska: oglas bez lokacije ili
     * bez razresene zone nema grad, a moderator bas to treba da vidi — takav oglas je korisniku
     * nevidljiv u pretrazi.
     */
    private String cityNameOf(Report report) {
        if (report.getLocation() == null || report.getLocation().getZone() == null) {
            return null;
        }
        return report.getLocation().getZone().getCity();
    }

    public AdminReportDetailsDTO getReportById(Long id) {
        Report report = reportRepository.findById(id)
                .filter(r -> r.getStatus() != ReportStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return mapToDetailsDTO(report);
    }

    @Transactional
    public void flagReport(Long id, String adminEmail) {
        Report report = reportRepository.findById(id)
                .filter(r -> r.getStatus() != ReportStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        report.setStatus(ReportStatus.FLAGGED);
        reportRepository.save(report);
        abuseReportService.resolveReportsForReport(id, adminEmail);
    }

    /**
     * Skida oznaku sa oglasa i vraca ga u status koji mu po podacima pripada.
     *
     * Ranije je bezuslovno vracao ACTIVE, pa je unflag umeo da ozivi oglas koji je pre
     * oznacavanja vec bio zavrsen ili istekao: oglas sa odobrenim claim-om vratio bi se u
     * pretragu i u matching za predmet koji je odavno vracen, a istekao bi ponovo poceo da
     * se prikazuje. Status se zato izvodi iz cinjenica, a ne pretpostavlja.
     */
    @Transactional
    public void unflagReport(Long id) {
        Report report = reportRepository.findById(id)
                .filter(r -> r.getStatus() == ReportStatus.FLAGGED)
                .orElseThrow(() -> new ResourceNotFoundException("Flagged report not found"));

        report.setStatus(restoredStatusFor(report));
        reportRepository.save(report);
    }

    private ReportStatus restoredStatusFor(Report report) {
        // Uslov na tip prati ClaimService.approveClaim: odobren claim zatvara samo PRONADJEN
        // oglas. Bez njega bi unflag vratio MATCHED na izgubljeni oglas i time ponistio
        // zastitu koja sprecava da trece lice zatvori tudji oglas.
        if (report.getType() == ReportType.FOUND
                && claimRepository.existsByReportIdAndStatus(report.getId(), ClaimStatus.APPROVED)) {
            return ReportStatus.MATCHED;
        }
        if (report.getExpiresAt() != null && report.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ReportStatus.EXPIRED;
        }
        return ReportStatus.ACTIVE;
    }

    private AdminReportDetailsDTO mapToDetailsDTO(Report report) {
        List<ReportImageDTO> imageDtos = report.getImages().stream()
                .map(img -> new ReportImageDTO(img.getId(), img.getImageUrl(), img.getDisplayOrder()))
                .toList();

        return new AdminReportDetailsDTO(
                report.getId(),
                report.getTitle(),
                report.getDescription(),
                report.getType(),
                report.getCategory().getId(),
                report.getCategory().getName(),
                report.getStatus(),
                LocationDTO.fromEntity(report.getLocation()),
                report.getCreatedAt(),
                report.getExpiresAt(),
                report.getUser().getId(),
                buildFullName(report.getUser()),
                report.getContactEmail(),
                report.getContactPhone(),
                imageDtos
        );
    }

    private String buildFullName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String combined = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        String trimmed = combined.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
