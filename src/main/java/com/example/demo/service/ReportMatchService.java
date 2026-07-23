package com.example.demo.service;

import com.example.demo.dto.LocationDTO;
import com.example.demo.dto.MatchDto;
import com.example.demo.dto.MatchReportSummaryDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Report;
import com.example.demo.model.ReportMatch;
import com.example.demo.model.ReportMatchStatus;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import com.example.demo.model.User;
import com.example.demo.repository.ReportMatchRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.UserRepository;
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
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MatchDto> getMatchesForReport(Long reportId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Report report = reportRepository.findById(reportId)
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        return reportMatchRepository
                .findVisibleByReportId(report.getId(), ReportMatchStatus.SUGGESTED, ReportStatus.ACTIVE)
                .stream()
                .map(match -> toDto(match, user.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchDto> getMyMatches(String userEmail, int limit) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ReportMatch match = reportMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));

        boolean ownsLost = match.getLostReport().getUser().getId().equals(user.getId());
        boolean ownsFound = match.getFoundReport().getUser().getId().equals(user.getId());

        if (!ownsLost && !ownsFound) {
            throw new ResourceNotFoundException("Match not found");
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
                match.getScore(),
                match.getDistanceKm().doubleValue(),
                match.getDistanceScore(),
                match.getTimeGapDays(),
                match.getTimeScore(),
                match.getTextScore(),
                match.getStatus(),
                match.getCreatedAt(),
                toSummary(myReport, viewerId),
                toSummary(otherReport, viewerId)
        );
    }

    private MatchReportSummaryDto toSummary(Report report, Long viewerId) {
        boolean hidesImages = report.getType() == ReportType.FOUND
                && !report.getUser().getId().equals(viewerId);
        String thumbnailUrl = report.getImages().isEmpty() || hidesImages
                ? null
                : report.getImages().getFirst().getImageUrl();

        return new MatchReportSummaryDto(
                report.getId(),
                report.getTitle(),
                report.getType(),
                report.getCategory().getName(),
                report.getCategory().getImageUrl(),
                report.getStatus(),
                LocationDTO.fromEntity(report.getLocation()),
                report.getCreatedAt(),
                thumbnailUrl
        );
    }
}
