package com.example.demo.report.service;

import com.example.demo.location.model.City;
import com.example.demo.location.LocationFacade;
import com.example.demo.report.dto.CommunityStatisticsDto;
import com.example.demo.report.repository.CommunityStatisticsView;
import com.example.demo.report.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class CommunityStatisticsService {

    private final ReportRepository reportRepository;
    private final LocationFacade locationFacade;

    @Value("${app.statistics.minimum-reunited:0}")
    private long minimumReunited;

    @Transactional(readOnly = true)
    public Optional<CommunityStatisticsDto> getForActiveCity(String userEmail) {
        City city = locationFacade.requireActiveCityOf(userEmail);
        CommunityStatisticsView statistics = reportRepository.findCommunityStatistics(city.getId());

        if (statistics.getReportsReunited() < minimumReunited) {
            return Optional.empty();
        }

        return Optional.of(new CommunityStatisticsDto(
                city.getName(),
                statistics.getReportsPosted(),
                statistics.getReportsReunited()
        ));
    }
}
