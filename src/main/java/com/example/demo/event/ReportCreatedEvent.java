package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ReportCreatedEvent extends ApplicationEvent {

    private final Long reportId;
    private final Long userId;
    private final String reportTitle;

    public ReportCreatedEvent(Object source, Long reportId, Long userId, String reportTitle) {
        super(source);
        this.reportId = reportId;
        this.userId = userId;
        this.reportTitle = reportTitle;
    }
}
