package com.example.demo.event;

import com.example.demo.model.NotificationType;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReportCreated(ReportCreatedEvent event) {
        log.info("ReportCreatedEvent received for report {}", event.getReportId());
        notificationService.createNotification(
                event.getUserId(),
                NotificationType.REPORT_CREATED,
                "Report Created",
                "Your report \"" + event.getReportTitle() + "\" has been published.",
                "{\"reportId\":" + event.getReportId() + "}"
        );
    }
}
