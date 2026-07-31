package com.example.demo.dto;

import com.example.demo.model.AbuseReason;
import com.example.demo.model.AbuseReportStatus;
import com.example.demo.model.AbuseTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Potvrda o primljenoj prijavi zloupotrebe, namenjena podnosiocu.
 *
 * Namerno NE nosi {@code targetLabel} iz {@link AbuseReportDto}. Ono je za prijavu korisnika
 * bilo njegovo ime i prezime, pa je svaki ulogovani korisnik slanjem golog {@code targetId}
 * mogao da procita pravo ime vlasnika tog naloga — do dnevnog limita od 10 identiteta. Podnosilac
 * tu vrednost ionako ne treba: sam je poslao id.
 *
 * Pun {@link AbuseReportDto} ostaje na {@code /admin/abuse-reports}, gde je ime neophodno za
 * moderaciju i gde pristup stiti rola.
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
