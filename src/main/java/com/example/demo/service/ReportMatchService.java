package com.example.demo.service;

import com.example.demo.dto.LocationDTO;
import com.example.demo.dto.MatchDto;
import com.example.demo.dto.MatchReportSummaryDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.DistanceBand;
import com.example.demo.model.Report;
import com.example.demo.model.ReportMatch;
import com.example.demo.model.ReportMatchStatus;
import com.example.demo.model.ReportStatus;
import com.example.demo.model.ReportType;
import com.example.demo.model.User;
import com.example.demo.model.Zone;
import com.example.demo.repository.ReportMatchRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.GeoUtils;
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
     * Opseg rastojanja izmedju centroida zona dva oglasa, ili null kad su oba oglasa u
     * ISTOJ zoni (tada "0 km" korisnik cita kao gresku — UI prikazuje samo naziv zone).
     *
     * Tacna distanca se NE izlaze klijentu: uz par oglasa na lokacijama koje korisnik sam
     * bira, ona bi trilateracijom odala tacnu lokaciju tudjeg oglasa. Tacna vrednost ostaje
     * u bazi (report_matches.distance_km) i koristi se samo za scoring i admin pregled.
     *
     * Opseg umesto broja: dok je zona bila cela opstina, "4.2 km" je bilo bezobrazno grubo
     * pa bezopasno. Sa zonama od oko 1 km2 vrednost "0.3 km" ostaje artefakt centroida sa
     * greskom reda pola kilometra, ali pocinje da IZGLEDA kao precizan podatak. To je
     * ispravka iskrenosti prikaza, ne mera zastite — naziv zone se ionako prikazuje, pa je
     * centroid napadacu poznat i bez distance.
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

        // Matchevi su pre-verifikacioni: svoj oglas se vidi tacno, tudji samo zonalno.
        LocationDTO location = isOwnReport
                ? LocationDTO.fromEntity(report.getLocation())
                : LocationDTO.zonalFromEntity(report.getLocation());

        return new MatchReportSummaryDto(
                report.getId(),
                report.getTitle(),
                report.getType(),
                report.getCategory().getName(),
                report.getCategory().getImageUrl(),
                report.getStatus(),
                location,
                report.getCreatedAt(),
                thumbnailUrl
        );
    }
}
