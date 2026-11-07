package com.example.demo.report.admin;

import com.example.demo.audit.AdminActionEvent;
import com.example.demo.audit.AuditAction;
import com.example.demo.audit.AuditTargetType;
import com.example.demo.location.dto.LocationDTO;
import com.example.demo.report.dto.ReportImageDTO;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;
import com.example.demo.report.repository.ReportRepository;
import com.example.demo.report.repository.ReportSpecifications;
import com.example.demo.report.service.AbuseReportService;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.shared.web.PageQuery;
import com.example.demo.user.model.User;
import com.example.demo.verification.VerificationFacade;
import com.example.demo.verification.model.ClaimStatus;
import com.example.demo.verification.service.ClaimService;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "createdAt", "createdAt",
            "title", "title",
            "status", "status",
            "type", "type",
            "id", "id");

    private final ReportRepository reportRepository;
    private final VerificationFacade verificationFacade;
    private final AbuseReportService abuseReportService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<AdminReportListDto> getReports(ReportType type, ReportStatus status, Long cityId, Long ownerId,
                                               String search, Integer page, Integer size, String sort, String direction) {
        Specification<Report> spec = Specification.allOf(
                ReportSpecifications.statusNot(ReportStatus.DELETED),
                ReportSpecifications.hasType(type),
                ReportSpecifications.hasStatus(status),
                ReportSpecifications.inCity(cityId),
                ReportSpecifications.userIdEquals(ownerId),
                ReportSpecifications.idOrTextContains(search),
                ReportSpecifications.withOwnerAndCategory(),
                ReportSpecifications.withLocationZone()
        );
        PageRequest pageRequest = PageQuery.of(page, size, sort, direction, SORTABLE_COLUMNS, "createdAt", Sort.Direction.DESC);
        return reportRepository.findAll(spec, pageRequest).map(this::mapToListDto);
    }

    private AdminReportListDto mapToListDto(Report report) {
        return new AdminReportListDto(
                report.getId(),
                report.getTitle(),
                report.getType(),
                report.getCategory().localizedName(LocaleContextHolder.getLocale()),
                report.getStatus(),
                LocationDTO.fromEntity(report.getLocation()),
                report.getCreatedAt(),
                report.getUser().getId(),
                buildFullName(report.getUser()),
                cityNameOf(report)
        );
    }

    /** Empty is a meaningful answer, not an error: a listing without a resolved zone has no city. */
    private String cityNameOf(Report report) {
        if (report.getLocation() == null || report.getLocation().getZone() == null) {
            return null;
        }
        return report.getLocation().getZone().getCity();
    }

    public AdminReportDetailsDTO getReportById(Long id) {
        Report report = reportRepository.findById(id)
                .filter(r -> r.getStatus() != ReportStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("report.notFound"));
        return mapToDetailsDTO(report);
    }

    @Transactional
    public void flagReport(Long id, String adminEmail) {
        Report report = reportRepository.findById(id)
                .filter(r -> r.getStatus() != ReportStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("report.notFound"));
        ReportStatus previousStatus = report.getStatus();
        report.setStatus(ReportStatus.FLAGGED);
        reportRepository.save(report);
        abuseReportService.resolveReportsForReport(id, adminEmail);
        eventPublisher.publishEvent(new AdminActionEvent(AuditAction.REPORT_HIDDEN, AuditTargetType.REPORT, id,
                "Hid report #" + id, Map.of("previousStatus", previousStatus.name())));
    }

    /**
     * Clears the flag and restores the status the data implies. Returning ACTIVE unconditionally
     * used to resurrect expired, closed and reunited listings alike.
     */
    @Transactional
    public void unflagReport(Long id) {
        Report report = reportRepository.findById(id)
                .filter(r -> r.getStatus() == ReportStatus.FLAGGED)
                .orElseThrow(() -> new ResourceNotFoundException("report.flaggedNotFound"));

        ReportStatus restoredStatus = restoredStatusFor(report);
        report.setStatus(restoredStatus);
        reportRepository.save(report);
        eventPublisher.publishEvent(new AdminActionEvent(AuditAction.REPORT_RESTORED, AuditTargetType.REPORT, id,
                "Restored report #" + id, Map.of("restoredStatus", restoredStatus.name())));
    }

    private ReportStatus restoredStatusFor(Report report) {
        // Reunion is remembered by a column, not a status, because the flag overwrote the status.
        if (report.getResolvedAt() != null) {
            return ReportStatus.RESOLVED;
        }
        
        if (report.getClosedAt() != null) {
            return ReportStatus.CLOSED;
        }
        // The type condition mirrors ClaimService.approveClaim: an approved claim closes only a
        // FOUND listing.
        if (report.getType() == ReportType.FOUND
                && verificationFacade.hasClaimWithStatus(report.getId(), ClaimStatus.APPROVED)) {
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
                report.getCategory().localizedName(LocaleContextHolder.getLocale()),
                report.getStatus(),
                LocationDTO.fromEntity(report.getLocation()),
                report.getCreatedAt(),
                report.getExpiresAt(),
                report.getUser().getId(),
                buildFullName(report.getUser()),
                report.getContactEmail(),
                report.getContactPhone(),
                imageDtos,
                report.getRewardAmount()
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
