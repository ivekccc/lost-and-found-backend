package com.example.demo.matching.service;

import com.example.demo.shared.embedding.TextEmbedder;

final class TextSimilarityBlend {

    static final double TRIGRAM_ONLY_SHARE = 1.0;

    private TextSimilarityBlend() {
    }

    static TextSignal combine(Double trigramSimilarity, float[] probeEmbedding, float[] candidateEmbedding,
                              double trigramShare, double semanticFloor) {
        double trigram = trigramSimilarity == null ? 0 : trigramSimilarity;
        if (probeEmbedding == null || candidateEmbedding == null) {
            return new TextSignal(trigram, null, trigram, TRIGRAM_ONLY_SHARE);
        }
        double semantic = TextEmbedder.cosineSimilarity(probeEmbedding, candidateEmbedding);
        double scaledSemantic = Math.max(0, (semantic - semanticFloor) / (1 - semanticFloor));
        return new TextSignal(trigram, semantic,
                trigramShare * trigram + (1 - trigramShare) * scaledSemantic, trigramShare);
    }

    record TextSignal(double trigram, Double semantic, double combined, double appliedTrigramShare) {
    }
}
