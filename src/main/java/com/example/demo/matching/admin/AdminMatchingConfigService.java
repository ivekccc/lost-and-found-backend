package com.example.demo.matching.admin;

import com.example.demo.audit.AdminActionEvent;
import com.example.demo.audit.AuditAction;
import com.example.demo.audit.AuditTargetType;
import com.example.demo.matching.model.MatchingConfig;
import com.example.demo.matching.repository.MatchingConfigRepository;
import com.example.demo.shared.embedding.TextEmbeddings;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminMatchingConfigService {

    private final MatchingConfigRepository matchingConfigRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TextEmbeddings textEmbeddings;

    @Transactional(readOnly = true)
    public MatchingConfigDto get() {
        return toDto(matchingConfigRepository.require());
    }

    @Transactional
    public MatchingConfigDto update(UpdateMatchingConfigRequestDto request) {
        MatchingConfig config = matchingConfigRepository.require();
        Map<String, Object> before = snapshot(config);

        config.setMaxDistanceKm(request.getMaxDistanceKm());
        config.setWeightDistance(request.getWeightDistance());
        config.setWeightText(request.getWeightText());
        config.setWeightTime(request.getWeightTime());
        config.setTimeDecayDays(request.getTimeDecayDays());
        config.setScoreThreshold(request.getScoreThreshold());
        config.setTrigramShare(request.getTrigramShare());
        config.setSemanticFloor(request.getSemanticFloor());
        config.setUpdatedAt(LocalDateTime.now());

        MatchingConfig saved = matchingConfigRepository.save(config);
        eventPublisher.publishEvent(new AdminActionEvent(AuditAction.MATCHING_CONFIG_UPDATED, AuditTargetType.MATCHING_CONFIG,
                null, "Updated matching parameters", Map.of("before", before, "after", snapshot(saved))));
        return toDto(saved);
    }

    private Map<String, Object> snapshot(MatchingConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("maxDistanceKm", config.getMaxDistanceKm());
        values.put("weightDistance", config.getWeightDistance());
        values.put("weightText", config.getWeightText());
        values.put("weightTime", config.getWeightTime());
        values.put("timeDecayDays", config.getTimeDecayDays());
        values.put("scoreThreshold", config.getScoreThreshold());
        values.put("trigramShare", config.getTrigramShare());
        values.put("semanticFloor", config.getSemanticFloor());
        return values;
    }

    private MatchingConfigDto toDto(MatchingConfig config) {
        return new MatchingConfigDto(
                config.getMaxDistanceKm(),
                config.getWeightDistance(),
                config.getWeightText(),
                config.getWeightTime(),
                config.getTimeDecayDays(),
                config.getScoreThreshold(),
                config.getTrigramShare(),
                config.getSemanticFloor(),
                textEmbeddings.isEnabled() ? textEmbeddings.modelName() : null,
                config.getUpdatedAt());
    }
}
