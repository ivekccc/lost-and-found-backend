package com.example.demo.matching.service;

import org.springframework.context.i18n.LocaleContextHolder;

import com.example.demo.location.dto.LocationDTO;
import com.example.demo.location.model.DistanceBand;
import com.example.demo.location.model.Zone;
import com.example.demo.matching.dto.MatchDto;
import com.example.demo.matching.dto.MatchReportSummaryDto;
import com.example.demo.matching.model.ReportMatch;
import com.example.demo.matching.model.ReportMatchStatus;
import com.example.demo.matching.repository.ReportMatchRepository;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;
import com.example.demo.report.ReportFacade;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.shared.util.GeoUtils;
import com.example.demo.user.model.User;
import com.example.demo.user.UserFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class ReportMatchService {

    private static final int MY_MATCHES_MAX_LIMIT = 10;

    private final ReportMatchRepository reportMatchRepository;
    private final ReportFacade reportFacade;
    private final UserFacade userFacade;

    @Transactional(readOnly = true)
    public List<MatchDto> getMatchesForReport(Long reportId, String userEmail) {
        User user = userFacade.requireByEmail(userEmail);

        Report report = reportFacade.findById(reportId)
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("report.notFound"));

        return reportMatchRepository
                .findVisibleByReportId(report.getId(), ReportMatchStatus.SUGGESTED, ReportStatus.ACTIVE)
                .stream()
                .map(match -> toDto(match, user.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchDto> getMyMatches(String userEmail, int limit) {
        User user = userFacade.requireByEmail(userEmail);

        int effectiveLimit = Math.min(Math.max(limit, 1), MY_MATCHES_MAX_LIMIT);

        return reportMatchRepository
                .findVisibleByUserId(user.getId(), ReportMatchStatus.SUGGESTED, ReportStatus.ACTIVE,
                        PageRequest.of(0, effectiveLimit))
                .stream()
                .map(match -> toDto(match, user.getId()))
                .toList();
    }

    @Transactional
    public void dismissMatch(Long matchId, String userEmail) {
        User user = userFacade.requireByEmail(userEmail);

        ReportMatch match = reportMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("match.notFound"));

        boolean ownsLost = match.getLostReport().getUser().getId().equals(user.getId());
        boolean ownsFound = match.getFoundReport().getUser().getId().equals(user.getId());

        if (!ownsLost && !ownsFound) {
            throw new ResourceNotFoundException("match.notFound");
        }

        if (ownsLost && match.getLostDismissedAt() == null) {
            match.setLostDismissedAt(LocalDateTime.now());
        }
        if (ownsFound && match.getFoundDismissedAt() == null) {
            match.setFoundDismissedAt(LocalDateTime.now());
        }

        reportMatchRepository.save(match);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getMatchCounts(List<Long> reportIds) {
        if (reportIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = new HashMap<>();
        mergeCounts(counts, reportMatchRepository.countVisibleByLostReportIds(
                reportIds, ReportMatchStatus.SUGGESTED, ReportStatus.ACTIVE));
        mergeCounts(counts, reportMatchRepository.countVisibleByFoundReportIds(
                reportIds, ReportMatchStatus.SUGGESTED, ReportStatus.ACTIVE));
        return counts;
    }

    private void mergeCounts(Map<Long, Long> counts, List<Object[]> rows) {
        for (Object[] row : rows) {
            counts.merge((Long) row[0], (Long) row[1], Long::sum);
        }
    }

    private MatchDto toDto(ReportMatch match, Long viewerId) {
        boolean viewerOwnsLost = match.getLostReport().getUser().getId().equals(viewerId);
        Report myReport = viewerOwnsLost ? match.getLostReport() : match.getFoundReport();
        Report otherReport = viewerOwnsLost ? match.getFoundReport() : match.getLostReport();

        return new MatchDto(
                match.getId(),
                roundScoreToTens(match.getScore()),
                zonalDistanceBand(myReport, otherReport),
                match.getTimeGapDays(),
                match.getStatus(),
                match.getCreatedAt(),
                toSummary(myReport, viewerId),
                toSummary(otherReport, viewerId)
        );
    }

    
    private Integer roundScoreToTens(Integer score) {
        return score == null ? null : (int) (Math.round(score / 10.0) * 10);
    }

    /**
     * Distance band between the two zones' centroids, or null when both listings sit in the SAME
     * zone — there "0 km" would read as a promise the data cannot keep.
     */
    private DistanceBand zonalDistanceBand(Report first, Report second) {
        Zone firstZone = first.getLocation() != null ? first.getLocation().getZone() : null;
        Zone secondZone = second.getLocation() != null ? second.getLocation().getZone() : null;

        if (firstZone == null || secondZone == null || firstZone.getId().equals(secondZone.getId())) {
            return null;
        }

        return DistanceBand.of(GeoUtils.haversineKm(
                firstZone.getCentroidLatitude().doubleValue(),
                firstZone.getCentroidLongitude().doubleValue(),
                secondZone.getCentroidLatitude().doubleValue(),
                secondZone.getCentroidLongitude().doubleValue()));
    }

    private MatchReportSummaryDto toSummary(Report report, Long viewerId) {
        boolean isOwnReport = report.getUser().getId().equals(viewerId);
        boolean hidesImages = report.getType() == ReportType.FOUND && !isOwnReport;
        String thumbnailUrl = report.getImages().isEmpty() || hidesImages
                ? null
                : report.getImages().getFirst().getImageUrl();

        // Matches are pre-verification: your own listing exact, the other one only by zone.
        LocationDTO location = isOwnReport
                ? LocationDTO.fromEntity(report.getLocation())
                : LocationDTO.zonalFromEntity(report.getLocation());

        return new MatchReportSummaryDto(
                report.getId(),
                report.getTitle(),
                report.getType(),
                report.getCategory().localizedName(LocaleContextHolder.getLocale()),
                report.getCategory().getImageUrl(),
                report.getStatus(),
                location,
                report.getCreatedAt(),
                thumbnailUrl
        );
    }
}
