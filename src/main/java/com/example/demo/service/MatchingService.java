package com.example.demo.service;

import com.example.demo.config.MatchingProperties;
import com.example.demo.model.NotificationType;
import com.example.demo.model.Report;
import com.example.demo.model.ReportMatch;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import com.example.demo.repository.MatchCandidateView;
import com.example.demo.repository.ReportMatchRepository;
import com.example.demo.repository.ReportRepository;
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

    private final ReportRepository reportRepository;
    private final ReportMatchRepository reportMatchRepository;
    private final NotificationService notificationService;
    private final MatchingProperties matchingProperties;

    @Transactional
    public void computeMatchesForReport(Long reportId) {
        Report report = reportRepository.findById(reportId).orElse(null);

        if (report == null || report.getStatus() != ReportStatus.ACTIVE || report.getLocation() == null) {
            return;
        }

        String probeText = buildProbeText(report);
        if (probeText.isBlank()) {
            return;
        }

        ReportType oppositeType = report.getType() == ReportType.LOST ? ReportType.FOUND : ReportType.LOST;

        List<MatchCandidateView> candidates = reportRepository.findMatchCandidates(
                oppositeType.name(),
                report.getCategory().getId(),
                report.getUser().getId(),
                probeText);

        for (MatchCandidateView candidate : candidates) {
            evaluateCandidate(report, candidate);
        }
    }

    private void evaluateCandidate(Report report, MatchCandidateView candidate) {
        double distanceKm = haversineKm(
                report.getLocation().getLatitude().doubleValue(),
                report.getLocation().getLongitude().doubleValue(),
                candidate.getLatitude().doubleValue(),
                candidate.getLongitude().doubleValue());

        if (distanceKm > matchingProperties.getMaxDistanceKm()) {
            return;
        }

        int timeGapDays = (int) Math.abs(Duration.between(report.getCreatedAt(), candidate.getCreatedAt()).toDays());
        double similarity = candidate.getSimilarity() == null ? 0 : candidate.getSimilarity();

        int distanceScore = (int) Math.round(matchingProperties.getWeightDistance()
                * (1 - Math.min(distanceKm, matchingProperties.getMaxDistanceKm()) / matchingProperties.getMaxDistanceKm()));
        int textScore = (int) Math.round(matchingProperties.getWeightText() * similarity);
        int timeScore = (int) Math.round(matchingProperties.getWeightTime()
                * Math.max(0, 1 - (double) timeGapDays / matchingProperties.getTimeDecayDays()));
        int score = distanceScore + textScore + timeScore;

        if (score < matchingProperties.getScoreThreshold()) {
            return;
        }

        Long lostReportId = report.getType() == ReportType.LOST ? report.getId() : candidate.getId();
        Long foundReportId = report.getType() == ReportType.FOUND ? report.getId() : candidate.getId();

        // A concurrent insert of the same pair aborts this transaction (unique constraint); the
        // periodic rescan reconciles anything lost to such a race, so no in-transaction retry.
        upsertMatch(lostReportId, foundReportId, score, distanceKm, distanceScore,
                timeGapDays, timeScore, similarity, textScore);
    }

    private void upsertMatch(Long lostReportId, Long foundReportId, int score, double distanceKm,
                             int distanceScore, int timeGapDays, int timeScore,
                             double similarity, int textScore) {
        ReportMatch match = reportMatchRepository
                .findByLostReportIdAndFoundReportId(lostReportId, foundReportId)
                .orElse(null);

        boolean isNew = match == null;

        if (isNew) {
            match = new ReportMatch();
            match.setLostReport(reportRepository.getReferenceById(lostReportId));
            match.setFoundReport(reportRepository.getReferenceById(foundReportId));
        }

        match.setScore(score);
        match.setDistanceKm(BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP));
        match.setDistanceScore(distanceScore);
        match.setTimeGapDays(timeGapDays);
        match.setTimeScore(timeScore);
        match.setTextSimilarity(BigDecimal.valueOf(similarity).setScale(3, RoundingMode.HALF_UP));
        match.setTextScore(textScore);

        if (isNew) {
            match.setNotifiedAt(LocalDateTime.now());
        }

        ReportMatch saved = reportMatchRepository.save(match);

        if (isNew) {
            notifyBothSides(saved);
        }
    }

    private void notifyBothSides(ReportMatch match) {
        Report lostReport = reportRepository.findById(match.getLostReport().getId()).orElse(null);
        Report foundReport = reportRepository.findById(match.getFoundReport().getId()).orElse(null);

        if (lostReport == null || foundReport == null) {
            return;
        }

        notificationService.createNotification(
                lostReport.getUser().getId(),
                NotificationType.MATCH_FOUND,
                "Possible match for your lost item",
                "A found item \"" + foundReport.getTitle() + "\" looks similar to your report \""
                        + lostReport.getTitle() + "\". Check it out.",
                matchDataJson(match.getId(), lostReport.getId(), foundReport.getId()));

        notificationService.createNotification(
                foundReport.getUser().getId(),
                NotificationType.MATCH_FOUND,
                "Someone may be looking for this item",
                "A lost report \"" + lostReport.getTitle() + "\" matches the item you found \""
                        + foundReport.getTitle() + "\".",
                matchDataJson(match.getId(), foundReport.getId(), lostReport.getId()));
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

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
