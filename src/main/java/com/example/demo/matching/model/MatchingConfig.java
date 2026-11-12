package com.example.demo.matching.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Live scoring parameters of the matching engine. Exactly one row, pinned to id 1 by a check
 * constraint, so the table is a settings record rather than a collection.
 */
@Entity
@Table(name = "matching_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchingConfig {

    public static final Long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "max_distance_km", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxDistanceKm;

    @Column(name = "weight_distance", nullable = false)
    private Integer weightDistance;

    @Column(name = "weight_text", nullable = false)
    private Integer weightText;

    @Column(name = "weight_time", nullable = false)
    private Integer weightTime;

    @Column(name = "time_decay_days", nullable = false)
    private Integer timeDecayDays;

    @Column(name = "score_threshold", nullable = false)
    private Integer scoreThreshold;

    @Column(name = "trigram_share", nullable = false, precision = 3, scale = 2)
    private BigDecimal trigramShare;

    @Column(name = "semantic_floor", nullable = false, precision = 3, scale = 2)
    private BigDecimal semanticFloor;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
