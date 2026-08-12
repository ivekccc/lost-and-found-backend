package com.example.demo.report.dto;

import com.example.demo.report.model.AbuseReason;
import com.example.demo.report.model.AbuseReportStatus;
import com.example.demo.report.model.AbuseTargetType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
/**
 * Receipt for the person who filed an abuse report.
 *
 * Deliberately carries no targetLabel: returning the full DTO handed the reported user's real name
 * to anyone who posted a bare {targetType, targetId}, up to the daily limit, with no trace for the
 * victim. The filer already knows the id they sent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AbuseReportReceiptDto",
        description = "Confirmation that a report was filed. Deliberately excludes any detail about "
                + "the reported target beyond what the reporter already supplied.")
public class AbuseReportReceiptDto {

    @NotNull
    private Long id;

    @NotNull
    private AbuseTargetType targetType;

    @NotNull
    private AbuseReason reason;

    @NotNull
    private AbuseReportStatus status;

    @NotNull
    private LocalDateTime createdAt;
}
