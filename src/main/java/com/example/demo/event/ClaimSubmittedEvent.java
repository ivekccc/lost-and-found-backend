package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClaimSubmittedEvent extends ApplicationEvent {

    private final Long claimId;
    private final Long challengeId;
    private final Long reportId;
    private final Long challengeAuthorId;
    private final String reportTitle;

    public ClaimSubmittedEvent(Object source, Long claimId, Long challengeId, Long reportId, Long challengeAuthorId, String reportTitle) {
        super(source);
        this.claimId = claimId;
        this.challengeId = challengeId;
        this.reportId = reportId;
        this.challengeAuthorId = challengeAuthorId;
        this.reportTitle = reportTitle;
    }
}
