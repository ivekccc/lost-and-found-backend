package com.example.demo.report.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class ReportImagesRemovedEvent extends ApplicationEvent {

    private final List<String> publicIds;

    public ReportImagesRemovedEvent(Object source, List<String> publicIds) {
        super(source);
        this.publicIds = publicIds;
    }
}
