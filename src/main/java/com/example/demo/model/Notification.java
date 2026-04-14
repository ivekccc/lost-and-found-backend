package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "data_json", columnDefinition = "TEXT")
    private String dataJson;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "push_status", nullable = false, length = 20)
    private PushStatus pushStatus = PushStatus.PENDING;

    @Column(name = "push_error", columnDefinition = "TEXT")
    private String pushError;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "sent_push_at")
    private LocalDateTime sentPushAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
