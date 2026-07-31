package com.example.demo.dto;

import com.example.demo.model.DistanceBand;
import com.example.demo.model.ReportMatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchDto {

    @NotNull
    private Long id;

    /**
     * Jacina meca 0-100, ZAOKRUZENA na najblizih 10. Sirov score nosi distanceScore
     * (1 poen = 625 m), pa bi uz nekoliko oglasa identicnog teksta istog dana razlike
     * u score-u multilateracijom odale lokaciju finije od zone. Tacan score ostaje u
     * bazi, koristi se za sortiranje na serveru i vidi se u AdminMatchListDto.
     */
    @NotNull
    @Schema(description = "Match strength 0-100, rounded to the nearest 10 so it cannot be "
            + "used to recover a more precise location than the zone.")
    private Integer score;

    /**
     * Opseg rastojanja izmedju CENTROIDA zona dva oglasa, ne izmedju tacnih lokacija —
     * inace bi korisnik, kreiranjem par oglasa na lokacijama koje sam bira, iz
     * distance trilateracijom rekonstruisao tacnu lokaciju tudjeg oglasa.
     *
     * Opseg umesto broja jer je zona sada oko 1 km2: tacna vrednost do centroida ima
     * gresku reda pola kilometra, pa bi "0.3 km" tvrdilo preciznost koja ne postoji.
     *
     * Null u dva slucaja: kad su oba oglasa u ISTOJ zoni (najcesce kod dobrih meceva —
     * tada je "0 km" obmanjujuce, UI prikazuje samo naziv zone) i kad zona nekog od
     * dva oglasa nije poznata.
     */
    @Schema(description = "How far apart the two reports' zones are, as a band rather than a number. "
            + "Measured between zone centroids, never between exact locations, so a one-decimal figure "
            + "would look far more precise than it is. Absent both when the two reports share the same "
            + "zone (compare the zone names on myReport/otherReport) and when either report's zone is "
            + "unknown.")
    private DistanceBand distanceBand;

    @NotNull
    private Integer timeGapDays;

    @NotNull
    private ReportMatchStatus status;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private MatchReportSummaryDto myReport;

    @NotNull
    private MatchReportSummaryDto otherReport;
}
