package com.example.demo.user.model;

import com.example.demo.location.model.City;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(name = "google_sub", length = 64, unique = true)
    private String googleSub;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "avatar_public_id")
    private String avatarPublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "active_city_id", nullable = false)
    private City activeCity;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 10)
    private AppLanguage language = AppLanguage.SR_LATN;

    @Column(name = "push_matches", nullable = false)
    private boolean pushMatches = true;

    @Column(name = "push_saved_searches", nullable = false)
    private boolean pushSavedSearches = true;

    @Column(name = "push_verification", nullable = false)
    private boolean pushVerification = true;

    @Column(name = "push_report_updates", nullable = false)
    private boolean pushReportUpdates = true;

    @Column(name = "push_messages", nullable = false)
    private boolean pushMessages = true;
}
