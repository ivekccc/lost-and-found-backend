package com.example.demo.report.dto;

import com.example.demo.location.dto.LocationDTO;
import com.example.demo.location.dto.ReportZoneDto;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;
import com.example.demo.verification.model.ClaimStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDetailsDTO {

    @NotNull
    private Long id;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private ReportType type;

    @NotNull
    private Long categoryId;

    @NotBlank
    private String categoryName;

    private String categoryImageUrl;

    @NotNull
    private ReportStatus status;

    private LocationDTO location;

    @NotNull
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    @NotNull
    private Long userId;

    private String userFullName;

    @NotNull
    private Boolean hasContactEmail;

    @NotNull
    private Boolean hasContactPhone;

    @Schema(description = "The contact email itself, only for the owner so the report can be edited; null for everyone else")
    private String contactEmail;

    @Schema(description = "The contact phone itself, only for the owner so the report can be edited; null for everyone else")
    private String contactPhone;

    private List<ReportImageDTO> images;

    @Schema(description = "Id of the challenge the report OWNER created on their own found report. "
            + "Only ever set for FOUND reports; this is the challenge a claimant answers.")
    private Long challengeId;

    @Schema(description = "Id of the challenge the VIEWER created on this report as a finder. Only ever "
            + "set for LOST reports. Present means the viewer already sent verification questions, so "
            + "they must not be offered the action again — and they are the one allowed to review the "
            + "claims on it.")
    private Long myChallengeId;

    @Schema(description = "Id of the viewer's most recent ownership claim on this report's challenge. "
            + "Absent when the viewer has never claimed.")
    private Long myClaimId;

    @Schema(description = "Status of the viewer's most recent ownership claim. Absent when the viewer "
            + "has never claimed. Clients must use this to decide whether to offer the claim action: "
            + "a PENDING or APPROVED claim means no further attempt is possible.")
    private ClaimStatus myClaimStatus;

    @Schema(description = "How many claims the viewer has already submitted on this report's challenge.")
    @NotNull
    private Integer myClaimAttemptsUsed;

    @Schema(description = "Server-side cap on claim attempts per challenge. Sent so clients do not "
            + "duplicate the rule and drift from it.")
    @NotNull
    private Integer maxClaimAttempts;

    @NotNull
    private Boolean reported;

    private ReportZoneDto zone;

    @NotNull
    @Schema(description = "Whether the caller has this listing saved for later. The client needs it "
            + "up front to show the right menu entry — offering \"Save\" on something already saved "
            + "is the same defect as offering an action the server would reject.")
    private Boolean saved;

    @NotNull
    @Schema(description = "Public link to this report's share page, safe to send to anyone")
    private String shareUrl;

    @NotNull
    @Schema(description = "True for the owner of an active report that expires within the reminder "
            + "window, when POST /reports/{id}/extend is offered.")
    private Boolean canExtend;

    @Schema(description = "Reward in dinars the owner offers to the finder; only ever set on LOST reports.")
    private Integer rewardAmount;

    @NotNull
    @Schema(description = "How many thank-yous the author of the report has received as a finder.")
    private Long authorThanksCount;
}
