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
import com.example.demo.matching.service.TextSimilarityBlend.TextSignal;
import com.example.demo.shared.util.GeoUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        computeQualifyingCandidates(reportId, false);
    }

    @Transactional
    public void recomputeMatchesForEditedReport(Long reportId) {
        Optional<Set<Long>> qualifying = computeQualifyingCandidates(reportId, true);
        if (qualifying.isEmpty()) {
            return;
        }
        List<ReportMatch> stale = reportMatchRepository
                .findByLostReportIdOrFoundReportId(reportId, reportId).stream()
                .filter(match -> !qualifying.get().contains(counterpartId(match, reportId)))
                .toList();
        if (!stale.isEmpty()) {
            log.info("Dropping {} matches of report {} that no longer qualify", stale.size(), reportId);
            reportMatchRepository.deleteAll(stale);
        }
    }

    private Long counterpartId(ReportMatch match, Long reportId) {
        return match.getLostReport().getId().equals(reportId)
                ? match.getFoundReport().getId()
                : match.getLostReport().getId();
    }

    private Optional<Set<Long>> computeQualifyingCandidates(Long reportId, boolean textMayHaveChanged) {
        Report report = reportFacade.findById(reportId).orElse(null);

        if (report == null || report.getStatus() != ReportStatus.ACTIVE || report.getLocation() == null) {
            return Optional.empty();
        }

        // A listing without a resolved zone has no city, so it cannot be compared to any candidate.
        Zone zone = report.getLocation().getZone();
        if (zone == null) {
            return Optional.empty();
        }

        String probeText = buildProbeText(report);
        if (probeText.isBlank()) {
            return Optional.empty();
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

        float[] probeEmbedding = textMayHaveChanged
                ? reportFacade.refreshTextEmbedding(reportId).orElse(null)
                : reportFacade.textEmbeddings(List.of(reportId)).get(reportId);
        Map<Long, float[]> candidateEmbeddings = probeEmbedding == null || candidates.isEmpty()
                ? Map.of()
                : reportFacade.textEmbeddings(candidates.stream().map(MatchCandidateView::getId).toList());

        Set<Long> qualifying = new HashSet<>();
        for (MatchCandidateView candidate : candidates) {
            TextSignal text = TextSimilarityBlend.combine(candidate.getSimilarity(), probeEmbedding,
                    candidateEmbeddings.get(candidate.getId()),
                    config.getTrigramShare().doubleValue(), config.getSemanticFloor().doubleValue());
            if (evaluateCandidate(report, candidate, text, config)) {
                qualifying.add(candidate.getId());
            }
        }
        return Optional.of(qualifying);
    }

    private boolean evaluateCandidate(Report report, MatchCandidateView candidate, TextSignal text,
                                      MatchingConfig config) {
        double distanceKm = GeoUtils.haversineKm(
                report.getLocation().getLatitude().doubleValue(),
                report.getLocation().getLongitude().doubleValue(),
                candidate.getLatitude().doubleValue(),
                candidate.getLongitude().doubleValue());

        if (distanceKm > config.getMaxDistanceKm().doubleValue()) {
            return false;
        }

        int timeGapDays = (int) Math.abs(Duration.between(report.getCreatedAt(), candidate.getCreatedAt()).toDays());
        double similarity = text.combined();

        double maxDistanceKm = config.getMaxDistanceKm().doubleValue();

        int distanceScore = (int) Math.round(config.getWeightDistance()
                * (1 - Math.min(distanceKm, maxDistanceKm) / maxDistanceKm));
        int textScore = (int) Math.round(config.getWeightText() * similarity);
        int timeScore = (int) Math.round(config.getWeightTime()
                * Math.max(0, 1 - (double) timeGapDays / config.getTimeDecayDays()));
        int score = distanceScore + textScore + timeScore;

        if (score < config.getScoreThreshold()) {
            return false;
        }

        Long lostReportId = report.getType() == ReportType.LOST ? report.getId() : candidate.getId();
        Long foundReportId = report.getType() == ReportType.FOUND ? report.getId() : candidate.getId();

        // A concurrent insert of the same pair aborts this transaction (unique constraint); the
        // periodic rescan reconciles anything lost to such a race, so no in-transaction retry.
        upsertMatch(lostReportId, foundReportId, score, distanceKm, distanceScore,
                timeGapDays, timeScore, text, textScore, config);
        return true;
    }

    private void upsertMatch(Long lostReportId, Long foundReportId, int score, double distanceKm,
                             int distanceScore, int timeGapDays, int timeScore,
                             TextSignal text, int textScore, MatchingConfig config) {
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
        match.setTextSimilarity(BigDecimal.valueOf(text.combined()).setScale(3, RoundingMode.HALF_UP));
        match.setTextScore(textScore);
        match.setTrigramSimilarity(BigDecimal.valueOf(text.trigram()).setScale(3, RoundingMode.HALF_UP));
        match.setSemanticSimilarity(text.semantic() == null
                ? null
                : BigDecimal.valueOf(text.semantic()).setScale(3, RoundingMode.HALF_UP));

        // Outside the isNew branch on purpose: a recomputed match must carry the parameters that
        // produced its current scores, or the stored row explains itself with the wrong numbers.
        match.setAppliedMaxDistanceKm(config.getMaxDistanceKm().setScale(2, RoundingMode.HALF_UP));
        match.setAppliedWeightDistance(config.getWeightDistance());
        match.setAppliedWeightText(config.getWeightText());
        match.setAppliedWeightTime(config.getWeightTime());
        match.setAppliedTimeDecayDays(config.getTimeDecayDays());
        match.setAppliedScoreThreshold(config.getScoreThreshold());
        match.setAppliedTrigramShare(BigDecimal.valueOf(text.appliedTrigramShare()).setScale(2, RoundingMode.HALF_UP));
        match.setAppliedSemanticFloor(config.getSemanticFloor().setScale(2, RoundingMode.HALF_UP));

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
