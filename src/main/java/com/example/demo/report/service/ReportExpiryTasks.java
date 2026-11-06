package com.example.demo.report.service;

import com.example.demo.notification.model.NotificationType;
import com.example.demo.notification.NotificationFacade;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;
import com.example.demo.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExpiryTasks {

    private final ReportRepository reportRepository;
    private final NotificationFacade notificationFacade;

    @Value("${app.reports.expiry-reminder-days:3}")
    private int expiryReminderDays;

    @Scheduled(fixedRateString = "${app.reports.expiry-sweep-interval-ms:3600000}")
    @Transactional
    public void remindBeforeExpiry() {
        LocalDateTime now = LocalDateTime.now();
        List<Report> expiring = reportRepository.findByStatusAndExpiresAtBetweenAndExpiryRemindedAtIsNull(
                ReportStatus.ACTIVE, now, now.plusDays(expiryReminderDays));

        if (expiring.isEmpty()) {
            return;
        }

        log.info("Reminding owners of {} reports about to expire", expiring.size());

        for (Report report : expiring) {
            report.setExpiryRemindedAt(now);
            reportRepository.save(report);

            String keyPrefix = report.getType() == ReportType.LOST
                    ? "notification.reportExpiringLost"
                    : "notification.reportExpiringFound";
            notificationFacade.notify(
                    report.getUser().getId(),
                    NotificationType.REPORT_EXPIRING,
                    keyPrefix + ".title",
                    keyPrefix + ".body",
                    "{\"reportId\":" + report.getId() + "}",
                    report.getTitle());
        }
    }

    @Scheduled(fixedRateString = "${app.reports.expiry-sweep-interval-ms:3600000}")
    @Transactional
    public void expireReports() {
        List<Report> expired = reportRepository.findByStatusAndExpiresAtBefore(
                ReportStatus.ACTIVE, LocalDateTime.now());

        if (expired.isEmpty()) {
            return;
        }

        log.info("Expiring {} reports", expired.size());

        for (Report report : expired) {
            report.setStatus(ReportStatus.EXPIRED);
            reportRepository.save(report);

            notificationFacade.notify(
                    report.getUser().getId(),
                    NotificationType.REPORT_EXPIRED,
                    "notification.reportExpired.title",
                    "notification.reportExpired.body",
                    "{\"reportId\":" + report.getId() + "}",
                    report.getTitle());
        }
    }
}
