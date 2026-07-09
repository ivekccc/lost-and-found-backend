package com.example.demo.service;

import com.example.demo.dto.AbuseReportDto;
import com.example.demo.dto.CreateAbuseReportRequestDto;
import com.example.demo.exception.InvalidAbuseReportException;
import com.example.demo.exception.RateLimitExceededException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.AbuseReport;
import com.example.demo.model.AbuseReportStatus;
import com.example.demo.model.AbuseTargetType;
import com.example.demo.model.Report;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.User;
import com.example.demo.repository.AbuseReportRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbuseReportService {

    private static final int MAX_REPORTS_PER_DAY = 10;
    private static final long DAY_IN_SECONDS = 86400;

    private final AbuseReportRepository abuseReportRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    @Transactional
    public AbuseReportDto createReport(String reporterEmail, CreateAbuseReportRequestDto request) {
        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AbuseReport report = new AbuseReport();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setReason(request.getReason());
        report.setMessage(request.getMessage());

        if (request.getTargetType() == AbuseTargetType.USER) {
            User targetUser = userRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reported user not found"));
            if (targetUser.getId().equals(reporter.getId())) {
                throw new InvalidAbuseReportException("You cannot report yourself");
            }
            if (abuseReportRepository.existsByReporterIdAndTargetUserIdAndStatus(
                    reporter.getId(), targetUser.getId(), AbuseReportStatus.PENDING)) {
                throw new InvalidAbuseReportException("You already have an open report for this user");
            }
            report.setTargetUser(targetUser);
        } else {
            Report targetReport = reportRepository.findById(request.getTargetId())
                    .filter(r -> r.getStatus() != ReportStatus.DELETED)
                    .orElseThrow(() -> new ResourceNotFoundException("Reported listing not found"));
            if (targetReport.getUser().getId().equals(reporter.getId())) {
                throw new InvalidAbuseReportException("You cannot report your own listing");
            }
            if (abuseReportRepository.existsByReporterIdAndTargetReportIdAndStatus(
                    reporter.getId(), targetReport.getId(), AbuseReportStatus.PENDING)) {
                throw new InvalidAbuseReportException("You already have an open report for this listing");
            }
            report.setTargetReport(targetReport);
        }

        long dailyCount = abuseReportRepository.countByReporterIdAndCreatedAtAfter(
                reporter.getId(), LocalDateTime.now().minusDays(1));
        if (dailyCount >= MAX_REPORTS_PER_DAY) {
            throw new RateLimitExceededException(
                    "Daily report limit reached. Try again tomorrow.", DAY_IN_SECONDS);
        }

        return toDto(abuseReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<AbuseReportDto> getReports(AbuseReportStatus status) {
        List<AbuseReport> reports = status != null
                ? abuseReportRepository.findByStatusOrderByCreatedAtDesc(status)
                : abuseReportRepository.findAllByOrderByCreatedAtDesc();
        return reports.stream().map(this::toDto).toList();
    }

    @Transactional
    public void dismiss(Long id, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        AbuseReport report = abuseReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abuse report not found"));
        if (report.getStatus() == AbuseReportStatus.PENDING) {
            markResolved(report, admin, AbuseReportStatus.DISMISSED);
            abuseReportRepository.save(report);
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
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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

    private AbuseReportDto toDto(AbuseReport report) {
        String targetLabel = report.getTargetType() == AbuseTargetType.USER
                ? buildDisplayName(report.getTargetUser())
                : report.getTargetReport().getTitle();

        return new AbuseReportDto(
                report.getId(),
                buildDisplayName(report.getReporter()),
                report.getTargetType(),
                report.getTargetUser() == null ? null : report.getTargetUser().getId(),
                report.getTargetReport() == null ? null : report.getTargetReport().getId(),
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
