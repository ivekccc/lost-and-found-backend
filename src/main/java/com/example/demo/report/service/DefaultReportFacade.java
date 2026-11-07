package com.example.demo.report.service;

import com.example.demo.report.CityListingCount;
import com.example.demo.report.ReportFacade;
import com.example.demo.report.dto.MatchCandidateView;
import com.example.demo.report.model.AbuseReportStatus;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportCategory;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;
import com.example.demo.report.repository.AbuseReportRepository;
import com.example.demo.report.repository.ReportCategoryRepository;
import com.example.demo.report.repository.ReportRepository;
import com.example.demo.report.repository.SavedReportRepository;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.shared.stats.DailyCount;
import com.example.demo.shared.stats.DailyCountView;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultReportFacade implements ReportFacade {

    private final ReportRepository reportRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final AbuseReportService abuseReportService;
    private final SavedReportRepository savedReportRepository;
    private final SavedSearchService savedSearchService;
    private final AbuseReportRepository abuseReportRepository;

    @Override
    @Transactional(readOnly = true)
    public Report requireById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("report.notFound"));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<Report> findById(Long reportId) {
        return reportRepository.findById(reportId);
    }

    @Override
    @Transactional(readOnly = true)
    public Report requireVisibleById(Long reportId) {
        return reportRepository.findById(reportId)
                .filter(report -> report.getStatus() != ReportStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("report.notFound"));
    }

    @Override
    @Transactional
    public Report lockById(Long reportId) {
        return reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("report.notFound"));
    }

    @Override
    public Report getReference(Long reportId) {
        return reportRepository.getReferenceById(reportId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchCandidateView> findMatchCandidates(String type, Long categoryId, Long ownerId,
                                                       String probeText, Long cityId) {
        return reportRepository.findMatchCandidates(type, categoryId, ownerId, probeText, cityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> findActiveLostListings() {
        return reportRepository.findByStatusAndType(ReportStatus.ACTIVE, ReportType.LOST);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportCategory requireCategoryById(Long categoryId) {
        return reportCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("category.notFound"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean categoryExists(Long categoryId) {
        return reportCategoryRepository.existsById(categoryId);
    }

    @Override
    @Transactional
    public void saveCategory(ReportCategory category) {
        reportCategoryRepository.save(category);
    }

    @Override
    @Transactional
    public List<Long> hideListingsOfBlockedUser(Long userId) {
        List<Long> hidden = new ArrayList<>();
        for (Report report : reportRepository.findByUserId(userId)) {
            if (report.getStatus() == ReportStatus.ACTIVE) {
                report.setStatus(ReportStatus.FLAGGED);
                report.setHiddenByUserBlock(true);
                reportRepository.save(report);
            }
            hidden.add(report.getId());
        }
        return hidden;
    }

    @Override
    @Transactional
    public void restoreListingsHiddenByBlock(Long userId) {
        for (Report report : reportRepository.findByUserId(userId)) {
            if (report.isHiddenByUserBlock()) {
                // Only lift what the block itself hid. A listing flagged by moderation before the
                // block must stay flagged, which is why the flag is recorded rather than guessed
                // from the status.
                if (report.getStatus() == ReportStatus.FLAGGED) {
                    report.setStatus(ReportStatus.ACTIVE);
                }
                report.setHiddenByUserBlock(false);
                reportRepository.save(report);
            }
        }
    }

    @Override
    @Transactional
    public List<String> deleteListingsOfUser(Long userId) {
        List<String> imagesToRemove = new ArrayList<>();
        for (Report report : reportRepository.findByUserId(userId)) {
            for (var image : report.getImages()) {
                if (image.getPublicId() != null) {
                    imagesToRemove.add(image.getPublicId());
                }
            }
            reportRepository.delete(report);
        }
        return imagesToRemove;
    }

    @Override
    @Transactional
    public void deleteSavedListingsOfUser(Long userId) {
        savedReportRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteSavedSearchesOfUser(Long userId) {
        savedSearchService.deleteSavedSearchesOfUser(userId);
    }

    @Override
    @Transactional
    public void resolveAbuseReportsForUser(Long userId, String adminEmail) {
        abuseReportService.resolveReportsForUser(userId, adminEmail);
    }

    @Override
    @Transactional
    public void dismissAbuseReportsForUser(Long userId, String adminEmail) {
        abuseReportService.dismissReportsForUser(userId, adminEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingAbuseReports() {
        return abuseReportRepository.countByStatus(AbuseReportStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> oldestPendingAbuseReportAt() {
        return Optional.ofNullable(abuseReportRepository.findOldestCreatedAt(AbuseReportStatus.PENDING));
    }

    @Override
    @Transactional(readOnly = true)
    public long countFlaggedListings() {
        return reportRepository.countByStatus(ReportStatus.FLAGGED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyCount> countListingsPostedPerDay(LocalDateTime from) {
        return reportRepository.countPostedPerDay(from).stream().map(DailyCountView::toDailyCount).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyCount> countListingsReunitedPerDay(LocalDateTime from) {
        return reportRepository.countReunitedPerDay(from).stream().map(DailyCountView::toDailyCount).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityListingCount> countListingsPerCity(LocalDateTime reunitedSince) {
        return reportRepository.countPerCity(reunitedSince).stream()
                .map(view -> new CityListingCount(view.getCityId(), view.getCityName(),
                        view.getActiveReports() == null ? 0 : view.getActiveReports(),
                        view.getReunitedReports() == null ? 0 : view.getReunitedReports()))
                .toList();
    }
}
