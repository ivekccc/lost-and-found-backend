package com.example.demo.report.event;

import com.example.demo.report.service.SavedSearchService;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SavedSearchEventListener {

    private final SavedSearchService savedSearchService;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReportCreated(ReportCreatedEvent event) {
        savedSearchService.notifySubscribers(event.getReportId());
    }
}
