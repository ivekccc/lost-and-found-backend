package com.example.demo.report.model;

import com.example.demo.location.model.Location;
import com.example.demo.user.model.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "reports")
@Getter
@Setter
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ReportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * When the owner marked the item reunited. A moment rather than a status: statistics ask WHEN,
     * and moderation overwrites the status.
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "hidden_by_user_block", nullable = false)
    private boolean hiddenByUserBlock;

   
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "report", orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = 50)
    private List<ReportImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
