package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClaimDecidedEvent extends ApplicationEvent {

    private final Long claimId;
    private final Long challengeId;
    private final Long reportId;
    private final Long claimantId;
    private final String reportTitle;
    private final boolean approved;

    public ClaimDecidedEvent(Object source, Long claimId, Long challengeId, Long reportId, Long claimantId, String reportTitle, boolean approved) {
        super(source);
        this.claimId = claimId;
        this.challengeId = challengeId;
        this.reportId = reportId;
        this.claimantId = claimantId;
        this.reportTitle = reportTitle;
        this.approved = approved;
    }
}
