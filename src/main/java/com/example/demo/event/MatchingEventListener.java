package com.example.demo.event;

import com.example.demo.service.MatchingService;
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
}
