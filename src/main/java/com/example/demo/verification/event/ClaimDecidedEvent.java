package com.example.demo.verification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
@Getter
public class ClaimDecidedEvent extends ApplicationEvent {

    private final Long claimId;
    private final Long challengeId;
    private final Long reportId;
    private final Long claimantId;
    private final Long holderId;
    private final String reportTitle;
    private final boolean approved;

    public ClaimDecidedEvent(Object source, Long claimId, Long challengeId, Long reportId, Long claimantId,
                             Long holderId, String reportTitle, boolean approved) {
        super(source);
        this.claimId = claimId;
        this.challengeId = challengeId;
        this.reportId = reportId;
        this.claimantId = claimantId;
        this.holderId = holderId;
        this.reportTitle = reportTitle;
        this.approved = approved;
    }
}
