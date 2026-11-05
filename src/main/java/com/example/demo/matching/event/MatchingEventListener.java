package com.example.demo.matching.event;

import com.example.demo.matching.service.MatchingService;
import com.example.demo.report.event.ReportCreatedEvent;
import com.example.demo.report.event.ReportUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingEventListener {

    private final MatchingService matchingService;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReportCreated(ReportCreatedEvent event) {
        log.info("Computing matches for new report {}", event.getReportId());
        matchingService.computeMatchesForReport(event.getReportId());
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReportUpdated(ReportUpdatedEvent event) {
        log.info("Recomputing matches for edited report {}", event.getReportId());
        matchingService.recomputeMatchesForEditedReport(event.getReportId());
    }
}
