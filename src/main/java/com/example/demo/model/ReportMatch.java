package com.example.demo.model;

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
