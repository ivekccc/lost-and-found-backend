package com.example.demo.matching.service;

import com.example.demo.location.model.Zone;
import com.example.demo.matching.model.MatchingConfig;
import com.example.demo.matching.model.ReportMatch;
import com.example.demo.matching.repository.MatchingConfigRepository;
import com.example.demo.report.dto.MatchCandidateView;
import com.example.demo.matching.repository.ReportMatchRepository;
import com.example.demo.notification.model.NotificationType;
import com.example.demo.notification.NotificationFacade;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;
import com.example.demo.report.ReportFacade;
import com.example.demo.shared.util.GeoUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final ReportFacade reportFacade;
    private final ReportMatchRepository reportMatchRepository;
    private final NotificationFacade notificationFacade;
    private final MatchingConfigRepository matchingConfigRepository;

    @Transactional
    public void computeMatchesForReport(Long reportId) {
        Report report = reportFacade.findById(reportId).orElse(null);

        if (report == null || report.getStatus() != ReportStatus.ACTIVE || report.getLocation() == null) {
            return;
        }

        // A listing without a resolved zone has no city, so it cannot be compared to any candidate.
        Zone zone = report.getLocation().getZone();
        if (zone == null) {
            return;
        }

        String probeText = buildProbeText(report);
        if (probeText.isBlank()) {
            return;
        }

        ReportType oppositeType = report.getType() == ReportType.LOST ? ReportType.FOUND : ReportType.LOST;

        List<MatchCandidateView> candidates = reportFacade.findMatchCandidates(
                oppositeType.name(),
                report.getCategory().getId(),
                report.getUser().getId(),
                probeText,
                zone.getCityId());

        // Read once per run rather than per candidate, and deliberately without a cache: an admin
        // edit must take effect on the next report, and one extra query per creation is nothing.
        MatchingConfig config = matchingConfigRepository.require();

        for (MatchCandidateView candidate : candidates) {
            evaluateCandidate(report, candidate, config);
        }
    }

    private void evaluateCandidate(Report report, MatchCandidateView candidate, MatchingConfig config) {
        double distanceKm = GeoUtils.haversineKm(
                report.getLocation().getLatitude().doubleValue(),
                report.getLocation().getLongitude().doubleValue(),
                candidate.getLatitude().doubleValue(),
                candidate.getLongitude().doubleValue());

        if (distanceKm > config.getMaxDistanceKm().doubleValue()) {
            return;
        }

        int timeGapDays = (int) Math.abs(Duration.between(report.getCreatedAt(), candidate.getCreatedAt()).toDays());
        double similarity = candidate.getSimilarity() == null ? 0 : candidate.getSimilarity();

        double maxDistanceKm = config.getMaxDistanceKm().doubleValue();

        int distanceScore = (int) Math.round(config.getWeightDistance()
                * (1 - Math.min(distanceKm, maxDistanceKm) / maxDistanceKm));
        int textScore = (int) Math.round(config.getWeightText() * similarity);
        int timeScore = (int) Math.round(config.getWeightTime()
                * Math.max(0, 1 - (double) timeGapDays / config.getTimeDecayDays()));
        int score = distanceScore + textScore + timeScore;

        if (score < config.getScoreThreshold()) {
            return;
        }

        Long lostReportId = report.getType() == ReportType.LOST ? report.getId() : candidate.getId();
        Long foundReportId = report.getType() == ReportType.FOUND ? report.getId() : candidate.getId();

        // A concurrent insert of the same pair aborts this transaction (unique constraint); the
        // periodic rescan reconciles anything lost to such a race, so no in-transaction retry.
        upsertMatch(lostReportId, foundReportId, score, distanceKm, distanceScore,
                timeGapDays, timeScore, similarity, textScore, config);
    }

    private void upsertMatch(Long lostReportId, Long foundReportId, int score, double distanceKm,
                             int distanceScore, int timeGapDays, int timeScore,
                             double similarity, int textScore, MatchingConfig config) {
        ReportMatch match = reportMatchRepository
                .findByLostReportIdAndFoundReportId(lostReportId, foundReportId)
                .orElse(null);

        boolean isNew = match == null;

        if (isNew) {
            match = new ReportMatch();
            match.setLostReport(reportFacade.getReference(lostReportId));
            match.setFoundReport(reportFacade.getReference(foundReportId));
        }

        match.setScore(score);
        match.setDistanceKm(BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP));
        match.setDistanceScore(distanceScore);
        match.setTimeGapDays(timeGapDays);
        match.setTimeScore(timeScore);
        match.setTextSimilarity(BigDecimal.valueOf(similarity).setScale(3, RoundingMode.HALF_UP));
        match.setTextScore(textScore);

        // Outside the isNew branch on purpose: a recomputed match must carry the parameters that
        // produced its current scores, or the stored row explains itself with the wrong numbers.
        match.setAppliedMaxDistanceKm(config.getMaxDistanceKm().setScale(2, RoundingMode.HALF_UP));
        match.setAppliedWeightDistance(config.getWeightDistance());
        match.setAppliedWeightText(config.getWeightText());
        match.setAppliedWeightTime(config.getWeightTime());
        match.setAppliedTimeDecayDays(config.getTimeDecayDays());
        match.setAppliedScoreThreshold(config.getScoreThreshold());

        if (isNew) {
            match.setNotifiedAt(LocalDateTime.now());
        }

        ReportMatch saved = reportMatchRepository.save(match);

        if (isNew) {
            notifyBothSides(saved);
        }
    }

    private void notifyBothSides(ReportMatch match) {
        Report lostReport = reportFacade.findById(match.getLostReport().getId()).orElse(null);
        Report foundReport = reportFacade.findById(match.getFoundReport().getId()).orElse(null);

        if (lostReport == null || foundReport == null) {
            return;
        }

        notificationFacade.notify(
                lostReport.getUser().getId(),
                NotificationType.MATCH_FOUND,
                "notification.matchForLost.title",
                "notification.matchForLost.body",
                matchDataJson(match.getId(), lostReport.getId(), foundReport.getId()),
                foundReport.getTitle(), lostReport.getTitle());

        notificationFacade.notify(
                foundReport.getUser().getId(),
                NotificationType.MATCH_FOUND,
                "notification.matchForFound.title",
                "notification.matchForFound.body",
                matchDataJson(match.getId(), foundReport.getId(), lostReport.getId()),
                lostReport.getTitle(), foundReport.getTitle());
    }

    private String matchDataJson(Long matchId, Long myReportId, Long otherReportId) {
        return "{\"matchId\":" + matchId + ",\"reportId\":" + myReportId
                + ",\"otherReportId\":" + otherReportId + "}";
    }

    private String buildProbeText(Report report) {
        String title = report.getTitle() == null ? "" : report.getTitle();
        String description = report.getDescription() == null ? "" : report.getDescription();
        return (title + " " + description).trim();
    }

}
