package com.example.demo.matching.service;

import com.example.demo.matching.service.TextSimilarityBlend.TextSignal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TextSimilarityBlendTest {

    private static final float[] EAST = {1, 0};
    private static final float[] NORTH_EAST = {(float) Math.sqrt(0.5), (float) Math.sqrt(0.5)};
    private static final float[] NORTH = {0, 1};

    @Test
    void blendsTrigramWithRescaledSemanticSimilarity() {
        TextSignal signal = TextSimilarityBlend.combine(0.38, EAST, NORTH_EAST, 0.5, 0.4);

        assertThat(signal.semantic()).isCloseTo(0.7071, within(0.0001));
        assertThat(signal.combined()).isCloseTo(0.5 * 0.38 + 0.5 * (0.7071 - 0.4) / 0.6, within(0.0001));
        assertThat(signal.appliedTrigramShare()).isEqualTo(0.5);
    }

    @Test
    void semanticSimilarityBelowTheFloorAddsNothing() {
        TextSignal signal = TextSimilarityBlend.combine(0.2, EAST, NORTH, 0.5, 0.4);

        assertThat(signal.semantic()).isZero();
        assertThat(signal.combined()).isCloseTo(0.1, within(0.0001));
    }

    @Test
    void fallsBackToTrigramWhenAnEmbeddingIsMissing() {
        TextSignal signal = TextSimilarityBlend.combine(0.3, EAST, null, 0.5, 0.4);

        assertThat(signal.semantic()).isNull();
        assertThat(signal.combined()).isEqualTo(0.3);
        assertThat(signal.appliedTrigramShare()).isEqualTo(TextSimilarityBlend.TRIGRAM_ONLY_SHARE);
    }

    @Test
    void treatsMissingTrigramSimilarityAsZero() {
        TextSignal signal = TextSimilarityBlend.combine(null, null, null, 0.5, 0.4);

        assertThat(signal.combined()).isZero();
    }
}
