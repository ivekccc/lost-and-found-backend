package com.example.demo.report.service;

import com.example.demo.audit.AdminActionEvent;
import com.example.demo.audit.AuditAction;
import com.example.demo.audit.AuditTargetType;
import com.example.demo.messaging.MessagingFacade;
import com.example.demo.report.dto.AbuseReportDto;
import com.example.demo.report.dto.AbuseReportReceiptDto;
import com.example.demo.report.dto.CreateAbuseReportRequestDto;
import com.example.demo.report.model.AbuseReason;
import com.example.demo.report.model.AbuseReport;
import com.example.demo.report.model.AbuseReportStatus;
import com.example.demo.report.model.AbuseTargetType;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.repository.AbuseReportRepository;
import com.example.demo.report.repository.AbuseReportSpecifications;
import com.example.demo.report.repository.ReportRepository;
import com.example.demo.shared.concurrency.AdvisoryLocks;
import com.example.demo.shared.concurrency.LockScope;
import com.example.demo.shared.exception.InvalidAbuseReportException;
import com.example.demo.shared.exception.RateLimitExceededException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.shared.web.PageQuery;
import com.example.demo.user.UserFacade;
import com.example.demo.user.model.User;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
public class AbuseReportService {

    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "createdAt", "createdAt",
            "status", "status",
            "reason", "reason",
            "id", "id");

    private static final int MAX_REPORTS_PER_DAY = 10;
    private static final long DAY_IN_SECONDS = 86400;

    private final AbuseReportRepository abuseReportRepository;
    private final UserFacade userFacade;
    private final ReportRepository reportRepository;
    private final MessagingFacade messagingFacade;
    private final AdvisoryLocks advisoryLocks;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AbuseReportReceiptDto createReport(String reporterEmail, CreateAbuseReportRequestDto request) {
        User reporter = userFacade.requireByEmail(reporterEmail);
        advisoryLocks.lock(LockScope.ABUSE_REPORT, reporter.getId());

        // The daily limit is checked BEFORE the target is looked up. At the end, a non-existent id
        // returned 404 while a real one returned 429 — which told the caller the id exists.
        long dailyCount = abuseReportRepository.countByReporterIdAndCreatedAtAfter(
                reporter.getId(), LocalDateTime.now().minusDays(1));
        if (dailyCount >= MAX_REPORTS_PER_DAY) {
            throw new RateLimitExceededException(
                    "abuse.dailyLimit", DAY_IN_SECONDS);
        }

        AbuseReport report = new AbuseReport();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setReason(request.getReason());
        report.setMessage(request.getMessage());

        if (request.getTargetType() == AbuseTargetType.USER) {
            User targetUser = userFacade.requireById(request.getTargetId());
            if (targetUser.getId().equals(reporter.getId())) {
                throw new InvalidAbuseReportException("abuse.cannotReportSelf");
            }
            if (abuseReportRepository.existsByReporterIdAndTargetUserIdAndStatus(
                    reporter.getId(), targetUser.getId(), AbuseReportStatus.PENDING)) {
                throw new InvalidAbuseReportException("abuse.alreadyOpenForUser");
            }
            report.setTargetUser(targetUser);
        } else if (request.getTargetType() == AbuseTargetType.MESSAGE) {
            MessagingFacade.ReportableMessage targetMessage = messagingFacade
                    .findReportableMessage(request.getTargetId(), reporter.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("abuse.messageNotFound"));
            if (abuseReportRepository.existsByReporterIdAndTargetMessageIdAndStatus(
                    reporter.getId(), targetMessage.messageId(), AbuseReportStatus.PENDING)) {
                throw new InvalidAbuseReportException("abuse.alreadyOpenForMessage");
            }
            report.setTargetMessageId(targetMessage.messageId());
            report.setTargetUser(userFacade.requireById(targetMessage.senderId()));
        } else {
            Report targetReport = reportRepository.findById(request.getTargetId())
                    .filter(r -> r.getStatus() != ReportStatus.DELETED)
                    .orElseThrow(() -> new ResourceNotFoundException("abuse.listingNotFound"));
            if (targetReport.getUser().getId().equals(reporter.getId())) {
                throw new InvalidAbuseReportException("abuse.cannotReportOwnListing");
            }
            if (abuseReportRepository.existsByReporterIdAndTargetReportIdAndStatus(
                    reporter.getId(), targetReport.getId(), AbuseReportStatus.PENDING)) {
                throw new InvalidAbuseReportException("abuse.alreadyOpenForListing");
            }
            report.setTargetReport(targetReport);
        }

        return toReceipt(abuseReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public Page<AbuseReportDto> getReports(AbuseReportStatus status, AbuseTargetType targetType, AbuseReason reason,
                                           Long reportId, Long userId,
                                           Integer page, Integer size, String sort, String direction) {
        Specification<AbuseReport> spec = Specification.allOf(
                AbuseReportSpecifications.hasStatus(status),
                AbuseReportSpecifications.hasTargetType(targetType),
                AbuseReportSpecifications.hasReason(reason),
                AbuseReportSpecifications.targetsReport(reportId),
                AbuseReportSpecifications.targetsUser(userId));
        PageRequest pageRequest = PageQuery.of(page, size, sort, direction, SORTABLE_COLUMNS, "createdAt", Sort.Direction.DESC);
        return abuseReportRepository.findAll(spec, pageRequest).map(this::toDto);
    }

    @Transactional
    public void dismiss(Long id, String adminEmail) {
        User admin = userFacade.requireByEmail(adminEmail);
        AbuseReport report = abuseReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("abuse.notFound"));
        if (report.getStatus() == AbuseReportStatus.PENDING) {
            markResolved(report, admin, AbuseReportStatus.DISMISSED);
            abuseReportRepository.save(report);
            eventPublisher.publishEvent(AdminActionEvent.of(AuditAction.ABUSE_REPORT_DISMISSED, AuditTargetType.ABUSE_REPORT, id,
                    "Dismissed abuse report #" + id));
        }
    }

    @Transactional
    public void resolveReportsForUser(Long targetUserId, String adminEmail) {
        resolvePending(abuseReportRepository.findByTargetUserIdAndStatus(targetUserId, AbuseReportStatus.PENDING),
                adminEmail, AbuseReportStatus.REVIEWED_ACTIONED);
    }

    @Transactional
    public void resolveReportsForReport(Long targetReportId, String adminEmail) {
        resolvePending(abuseReportRepository.findByTargetReportIdAndStatus(targetReportId, AbuseReportStatus.PENDING),
                adminEmail, AbuseReportStatus.REVIEWED_ACTIONED);
    }

    @Transactional
    public void dismissReportsForUser(Long targetUserId, String adminEmail) {
        resolvePending(abuseReportRepository.findByTargetUserIdAndStatus(targetUserId, AbuseReportStatus.PENDING),
                adminEmail, AbuseReportStatus.DISMISSED);
    }

    private void resolvePending(List<AbuseReport> reports, String adminEmail, AbuseReportStatus resolution) {
        if (reports.isEmpty()) {
            return;
        }
        User admin = userFacade.requireByEmail(adminEmail);
        for (AbuseReport report : reports) {
            markResolved(report, admin, resolution);
        }
        abuseReportRepository.saveAll(reports);
    }

    private void markResolved(AbuseReport report, User admin, AbuseReportStatus resolution) {
        report.setStatus(resolution);
        report.setReviewedBy(admin);
        report.setReviewedAt(LocalDateTime.now());
    }

    /** Receipt for the filer, carrying nothing about the target they did not already send. */
    private AbuseReportReceiptDto toReceipt(AbuseReport report) {
        return new AbuseReportReceiptDto(
                report.getId(),
                report.getTargetType(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }

    private AbuseReportDto toDto(AbuseReport report) {
        MessagingFacade.ReportableMessage targetMessage = report.getTargetMessageId() == null
                ? null
                : messagingFacade.describeMessage(report.getTargetMessageId()).orElse(null);
        String targetLabel = switch (report.getTargetType()) {
            case USER -> buildDisplayName(report.getTargetUser());
            case REPORT -> report.getTargetReport().getTitle();
            case MESSAGE -> targetMessage != null && !targetMessage.excerpt().isBlank()
                    ? targetMessage.excerpt()
                    : "#" + report.getId();
        };

        return new AbuseReportDto(
                report.getId(),
                buildDisplayName(report.getReporter()),
                report.getTargetType(),
                report.getTargetUser() == null ? null : report.getTargetUser().getId(),
                report.getTargetReport() == null ? null : report.getTargetReport().getId(),
                report.getTargetMessageId(),
                targetMessage == null ? null : targetMessage.conversationId(),
                targetLabel,
                report.getReason(),
                report.getMessage(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getReviewedBy() == null ? null : buildDisplayName(report.getReviewedBy()),
                report.getReviewedAt(),
                report.getResolutionNote()
        );
    }

    private String buildDisplayName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String combined = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        String trimmed = combined.trim();
        return trimmed.isEmpty() ? user.getUsername() : trimmed;
    }
}
