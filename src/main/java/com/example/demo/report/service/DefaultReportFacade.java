package com.example.demo.report.service;

import com.example.demo.report.ReportFacade;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportCategory;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.model.ReportType;
import com.example.demo.report.dto.MatchCandidateView;
import com.example.demo.report.repository.ReportCategoryRepository;
import com.example.demo.report.repository.ReportRepository;
import com.example.demo.report.repository.SavedReportRepository;
import com.example.demo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultReportFacade implements ReportFacade {

    private final ReportRepository reportRepository;
    private final ReportCategoryRepository reportCategoryRepository;
    private final AbuseReportService abuseReportService;
    private final SavedReportRepository savedReportRepository;

    @Override
    @Transactional(readOnly = true)
    public Report requireById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report with id " + reportId + " not found"));
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
                .orElseThrow(() -> new ResourceNotFoundException("Report with id " + reportId + " not found"));
    }

    @Override
    @Transactional
    public Report lockById(Long reportId) {
        return reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
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
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
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
    public void resolveAbuseReportsForUser(Long userId, String adminEmail) {
        abuseReportService.resolveReportsForUser(userId, adminEmail);
    }

    @Override
    @Transactional
    public void dismissAbuseReportsForUser(Long userId, String adminEmail) {
        abuseReportService.dismissReportsForUser(userId, adminEmail);
    }
}
