package com.example.demo.report.service;

import com.example.demo.location.LocationFacade;
import com.example.demo.location.model.Location;
import com.example.demo.messaging.MessagingFacade;
import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportImage;
import com.example.demo.report.model.ReportStatus;
import com.example.demo.report.repository.ReportRepository;
import com.example.demo.shared.media.CloudinaryService;
import com.example.demo.verification.VerificationFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportRetentionTasks {

    private static final int BATCH_SIZE = 100;

    private final ReportRepository reportRepository;
    private final VerificationFacade verificationFacade;
    private final MessagingFacade messagingFacade;
    private final LocationFacade locationFacade;
    private final CloudinaryService cloudinaryService;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.reports.deleted-retention-days:30}")
    private int deletedRetentionDays;

    @Scheduled(fixedRateString = "${app.reports.retention-sweep-interval-ms:21600000}", initialDelayString = "PT5M")
    public void purgeDeletedListings() {
        LocalDateTime before = LocalDateTime.now().minusDays(deletedRetentionDays);
        List<Long> reportIds = reportRepository.findIdsDeletedBefore(ReportStatus.DELETED, before, PageRequest.of(0, BATCH_SIZE));
        int purged = 0;
        for (Long reportId : reportIds) {
            try {
                List<String> images = transactionTemplate.execute(status -> purge(reportId));
                if (images != null) {
                    cloudinaryService.deleteImages(images);
                    purged++;
                }
            } catch (RuntimeException e) {
                log.error("Could not purge deleted listing {}", reportId, e);
            }
        }
        if (purged > 0) {
            log.info("Purged {} listings deleted more than {} days ago", purged, deletedRetentionDays);
        }
    }

    private List<String> purge(Long reportId) {
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null || report.getStatus() != ReportStatus.DELETED) {
            return null;
        }
        List<String> images = new ArrayList<>();
        report.getImages().stream()
                .map(ReportImage::getPublicId)
                .filter(publicId -> publicId != null)
                .forEach(images::add);
        if (messagingFacade.hasThanksOnListing(reportId)) {
            images.addAll(verificationFacade.scrubClaimsForListing(reportId));
            report.getImages().clear();
            report.setContactEmail(null);
            report.setContactPhone(null);
            report.setDeletedAt(null);
            return images;
        }
        images.addAll(verificationFacade.deleteVerificationDataForListing(reportId));
        Location location = report.getLocation();
        reportRepository.delete(report);
        reportRepository.flush();
        locationFacade.deleteLocation(location);
        return images;
    }
}
