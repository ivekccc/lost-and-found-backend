package com.example.demo.matching.admin;

import com.example.demo.matching.model.MatchingConfig;
import com.example.demo.matching.repository.MatchingConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminMatchingConfigService {

    private final MatchingConfigRepository matchingConfigRepository;

    @Transactional(readOnly = true)
    public MatchingConfigDto get() {
        return toDto(matchingConfigRepository.require());
    }

    @Transactional
    public MatchingConfigDto update(UpdateMatchingConfigRequestDto request) {
        MatchingConfig config = matchingConfigRepository.require();

        config.setMaxDistanceKm(request.getMaxDistanceKm());
        config.setWeightDistance(request.getWeightDistance());
        config.setWeightText(request.getWeightText());
        config.setWeightTime(request.getWeightTime());
        config.setTimeDecayDays(request.getTimeDecayDays());
        config.setScoreThreshold(request.getScoreThreshold());
        config.setUpdatedAt(LocalDateTime.now());

        // Matches already stored keep the parameters they were scored with; these values only
        // affect runs from here on.
        return toDto(matchingConfigRepository.save(config));
    }

    private MatchingConfigDto toDto(MatchingConfig config) {
        return new MatchingConfigDto(
                config.getMaxDistanceKm(),
                config.getWeightDistance(),
                config.getWeightText(),
                config.getWeightTime(),
                config.getTimeDecayDays(),
                config.getScoreThreshold(),
                config.getUpdatedAt());
    }
}
