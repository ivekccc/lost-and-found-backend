package com.example.demo.service;

import com.example.demo.dto.CommunityStatisticsDto;
import com.example.demo.model.City;
import com.example.demo.repository.CommunityStatisticsView;
import com.example.demo.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunityStatisticsService {

    private final ReportRepository reportRepository;
    private final CityService cityService;

    @Value("${app.statistics.minimum-reunited:0}")
    private long minimumReunited;

    @Transactional(readOnly = true)
    public Optional<CommunityStatisticsDto> getForActiveCity(String userEmail) {
        City city = cityService.getActiveCity(userEmail);
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
