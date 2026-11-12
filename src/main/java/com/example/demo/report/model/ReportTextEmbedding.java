package com.example.demo.report.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_text_embeddings")
@Data
@NoArgsConstructor
public class ReportTextEmbedding {

    @Id
    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "model", nullable = false, length = 120)
    private String model;

    @Column(name = "text_hash", nullable = false, length = 64)
    private String textHash;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "embedding", nullable = false, columnDefinition = "real[]")
    private float[] embedding;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
