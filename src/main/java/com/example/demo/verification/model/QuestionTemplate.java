package com.example.demo.verification.model;

import com.example.demo.report.model.ReportCategory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
@Entity
@Table(name = "question_templates")
@Getter
@Setter
public class QuestionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ReportCategory category;

    @Column(nullable = false, length = 500)
    private String prompt;

    @Column(name = "prompt_sr", nullable = false, length = 500)
    private String promptSr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionKind kind;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_choices", columnDefinition = "jsonb")
    private List<String> defaultChoices;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_choices_sr", columnDefinition = "jsonb")
    private List<String> defaultChoicesSr;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String localizedPrompt(Locale locale) {
        return isSerbian(locale) ? promptSr : prompt;
    }

    public List<String> localizedDefaultChoices(Locale locale) {
        return isSerbian(locale) ? defaultChoicesSr : defaultChoices;
    }

    private static boolean isSerbian(Locale locale) {
        return locale != null && "sr".equals(locale.getLanguage());
    }
}
