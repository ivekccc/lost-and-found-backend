package com.example.demo.matching.model;

import com.example.demo.report.model.Report;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(name = "report_matches")
@Data
@NoArgsConstructor
public class ReportMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_report_id", nullable = false)
    private Report lostReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "found_report_id", nullable = false)
    private Report foundReport;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "distance_km", nullable = false, precision = 6, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "distance_score", nullable = false)
    private Integer distanceScore;

    @Column(name = "time_gap_days", nullable = false)
    private Integer timeGapDays;

    @Column(name = "time_score", nullable = false)
    private Integer timeScore;

    @Column(name = "text_similarity", nullable = false, precision = 4, scale = 3)
    private BigDecimal textSimilarity;

    @Column(name = "text_score", nullable = false)
    private Integer textScore;

    @Column(name = "trigram_similarity", nullable = false, precision = 4, scale = 3)
    private BigDecimal trigramSimilarity;

    @Column(name = "semantic_similarity", precision = 4, scale = 3)
    private BigDecimal semanticSimilarity;

    /**
     * Scoring parameters as they were at computation time, not as they are now. A match read back
     * later must explain its own score even after the configuration has been retuned.
     */
    @Column(name = "applied_max_distance_km", nullable = false, precision = 6, scale = 2)
    private BigDecimal appliedMaxDistanceKm;

    @Column(name = "applied_weight_distance", nullable = false)
    private Integer appliedWeightDistance;

    @Column(name = "applied_weight_text", nullable = false)
    private Integer appliedWeightText;

    @Column(name = "applied_weight_time", nullable = false)
    private Integer appliedWeightTime;

    @Column(name = "applied_time_decay_days", nullable = false)
    private Integer appliedTimeDecayDays;

    @Column(name = "applied_score_threshold", nullable = false)
    private Integer appliedScoreThreshold;

    @Column(name = "applied_trigram_share", nullable = false, precision = 3, scale = 2)
    private BigDecimal appliedTrigramShare;

    @Column(name = "applied_semantic_floor", nullable = false, precision = 3, scale = 2)
    private BigDecimal appliedSemanticFloor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportMatchStatus status = ReportMatchStatus.SUGGESTED;

    @Column(name = "lost_dismissed_at")
    private LocalDateTime lostDismissedAt;

    @Column(name = "found_dismissed_at")
    private LocalDateTime foundDismissedAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
