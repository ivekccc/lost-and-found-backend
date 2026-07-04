package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChallengeCreatedEvent extends ApplicationEvent {

    private final Long challengeId;
    private final Long reportId;
    private final Long reportOwnerId;
    private final String reportTitle;

    public ChallengeCreatedEvent(Object source, Long challengeId, Long reportId, Long reportOwnerId, String reportTitle) {
        super(source);
        this.challengeId = challengeId;
        this.reportId = reportId;
        this.reportOwnerId = reportOwnerId;
        this.reportTitle = reportTitle;
    }
}
