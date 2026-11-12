package com.example.demo.report.repository;

import com.example.demo.report.model.ReportTextEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportTextEmbeddingRepository extends JpaRepository<ReportTextEmbedding, Long> {

    @Modifying
    @Query(value = "INSERT INTO report_text_embeddings (report_id, model, text_hash, embedding, created_at) "
            + "VALUES (:reportId, :model, :textHash, CAST(:embedding AS real[]), NOW()) "
            + "ON CONFLICT (report_id) DO UPDATE "
            + "SET model = EXCLUDED.model, text_hash = EXCLUDED.text_hash, "
            + "embedding = EXCLUDED.embedding, created_at = EXCLUDED.created_at",
            nativeQuery = true)
    void upsert(@Param("reportId") Long reportId,
                @Param("model") String model,
                @Param("textHash") String textHash,
                @Param("embedding") String embedding);
}
