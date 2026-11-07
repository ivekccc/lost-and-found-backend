package com.example.demo.dashboard.service;

import com.example.demo.dashboard.dto.ActivityDayDto;
import com.example.demo.dashboard.dto.CityOverviewDto;
import com.example.demo.dashboard.dto.QueueCountsDto;
import com.example.demo.report.ReportFacade;
import com.example.demo.shared.stats.DailyCount;
import com.example.demo.user.UserFacade;
import com.example.demo.verification.VerificationFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final int MIN_DAYS = 7;
    private static final int MAX_DAYS = 90;

    private final ReportFacade reportFacade;
    private final VerificationFacade verificationFacade;
    private final UserFacade userFacade;

    @Transactional(readOnly = true)
    public QueueCountsDto getQueues() {
        return new QueueCountsDto(
                reportFacade.countPendingAbuseReports(),
                reportFacade.oldestPendingAbuseReportAt().orElse(null),
                reportFacade.countFlaggedListings(),
                verificationFacade.countPendingClaims(),
                verificationFacade.oldestPendingClaimAt().orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ActivityDayDto> getActivity(int days) {
        int span = Math.min(Math.max(days, MIN_DAYS), MAX_DAYS);
        LocalDate firstDay = LocalDate.now().minusDays(span - 1L);
        LocalDateTime from = firstDay.atStartOfDay();

        Map<LocalDate, Long> posted = byDay(reportFacade.countListingsPostedPerDay(from));
        Map<LocalDate, Long> reunited = byDay(reportFacade.countListingsReunitedPerDay(from));
        Map<LocalDate, Long> registered = byDay(userFacade.countRegistrationsPerDay(from));

        List<ActivityDayDto> result = new ArrayList<>(span);
        for (int offset = 0; offset < span; offset++) {
            LocalDate day = firstDay.plusDays(offset);
            result.add(new ActivityDayDto(
                    day,
                    posted.getOrDefault(day, 0L),
                    reunited.getOrDefault(day, 0L),
                    registered.getOrDefault(day, 0L)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CityOverviewDto> getCities(int days) {
        int span = Math.min(Math.max(days, MIN_DAYS), MAX_DAYS);
        LocalDateTime from = LocalDate.now().minusDays(span - 1L).atStartOfDay();
        return reportFacade.countListingsPerCity(from).stream()
                .map(city -> new CityOverviewDto(city.cityId(), city.cityName(), city.activeListings(), city.reunitedListings()))
                .toList();
    }

    private Map<LocalDate, Long> byDay(List<DailyCount> counts) {
        return counts.stream().collect(Collectors.toMap(DailyCount::day, DailyCount::count, Long::sum));
    }
}
