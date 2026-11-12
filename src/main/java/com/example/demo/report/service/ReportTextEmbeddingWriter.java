package com.example.demo.report.service;

import com.example.demo.report.repository.ReportTextEmbeddingRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportTextEmbeddingWriter {

    private final ReportTextEmbeddingRepository reportTextEmbeddingRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void store(String model, List<ComputedEmbedding> embeddings) {
        embeddings.stream()
                .sorted(Comparator.comparing(ComputedEmbedding::reportId))
                .forEach(embedding -> reportTextEmbeddingRepository.upsert(
                        embedding.reportId(), model, embedding.textHash(), arrayLiteral(embedding.vector())));
    }

    static String arrayLiteral(float[] vector) {
        StringBuilder literal = new StringBuilder(vector.length * 12).append('{');
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                literal.append(',');
            }
            literal.append(vector[index]);
        }
        return literal.append('}').toString();
    }

    public record ComputedEmbedding(Long reportId, String textHash, float[] vector) {
    }
}
