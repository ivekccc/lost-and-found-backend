package com.example.demo.report.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ReportUpdatedEvent extends ApplicationEvent {

    private final Long reportId;

    public ReportUpdatedEvent(Object source, Long reportId) {
        super(source);
        this.reportId = reportId;
    }
}
