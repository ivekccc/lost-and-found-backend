package com.example.demo.report.service;

import com.example.demo.notification.model.NotificationType;
import com.example.demo.notification.NotificationFacade;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                    "Your report expired",
                    "Your report \"" + report.getTitle() + "\" has expired and is no longer visible "
                            + "to others. Reopen it if you are still looking, or mark it reunited "
                            + "if the item is back.",
                    "{\"reportId\":" + report.getId() + "}");
        }
    }
}
