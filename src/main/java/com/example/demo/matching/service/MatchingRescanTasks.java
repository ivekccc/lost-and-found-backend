package com.example.demo.matching.service;

import com.example.demo.report.ReportFacade;
import com.example.demo.report.model.Report;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingRescanTasks {

    private final ReportFacade reportFacade;
    private final MatchingService matchingService;

    @Scheduled(fixedDelayString = "${app.matching.rescan-interval-ms:21600000}")
    public void rescanMatches() {
        List<Report> lostReports = reportFacade.findActiveLostListings();
        log.info("Matching rescan over {} active lost reports", lostReports.size());

        // Each report gets its own transaction through the service proxy, so one failed
        // report (e.g. a unique-constraint race with the create listener) cannot roll back
        // or abort the rest of the rescan.
        for (Report lostReport : lostReports) {
            try {
                matchingService.computeMatchesForReport(lostReport.getId());
            } catch (Exception e) {
                log.error("Matching rescan failed for report {}", lostReport.getId(), e);
            }
        }
    }
}
