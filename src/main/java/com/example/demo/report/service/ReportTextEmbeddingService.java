package com.example.demo.report.service;

import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportTextEmbedding;
import com.example.demo.report.repository.ReportRepository;
import com.example.demo.report.repository.ReportTextEmbeddingRepository;
import com.example.demo.report.service.ReportTextEmbeddingWriter.ComputedEmbedding;
import com.example.demo.shared.embedding.TextEmbeddings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportTextEmbeddingService {

    private final TextEmbeddings textEmbeddings;
    private final ReportTextEmbeddingRepository reportTextEmbeddingRepository;
    private final ReportRepository reportRepository;
    private final ReportTextEmbeddingWriter reportTextEmbeddingWriter;

    public Optional<float[]> refresh(Long reportId) {
        return Optional.ofNullable(current(List.of(reportId)).get(reportId));
    }

    public Map<Long, float[]> current(Collection<Long> reportIds) {
        Map<Long, float[]> embeddings = new HashMap<>();
        if (!textEmbeddings.isEnabled() || reportIds.isEmpty()) {
            return embeddings;
        }
        String model = textEmbeddings.modelName();
        Map<Long, ReportTextEmbedding> stored = reportTextEmbeddingRepository.findAllById(reportIds).stream()
                .collect(Collectors.toMap(ReportTextEmbedding::getReportId, Function.identity()));
        List<ComputedEmbedding> computed = new ArrayList<>();
        for (Report report : reportRepository.findAllById(reportIds)) {
            String text = textOf(report);
            String textHash = hash(text);
            ReportTextEmbedding existing = stored.get(report.getId());
            if (existing != null && model.equals(existing.getModel()) && textHash.equals(existing.getTextHash())) {
                embeddings.put(report.getId(), existing.getEmbedding());
                continue;
            }
            textEmbeddings.embed(text).ifPresent(vector -> {
                embeddings.put(report.getId(), vector);
                computed.add(new ComputedEmbedding(report.getId(), textHash, vector));
            });
        }
        if (!computed.isEmpty()) {
            try {
                reportTextEmbeddingWriter.store(model, computed);
            } catch (DataAccessException | TransactionException exception) {
                log.warn("Text embeddings of {} reports were computed but not stored", computed.size(), exception);
            }
        }
        return embeddings;
    }

    private static String textOf(Report report) {
        String title = report.getTitle() == null ? "" : report.getTitle();
        String description = report.getDescription() == null ? "" : report.getDescription();
        return (title + ". " + description).trim();
    }

    private static String hash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
