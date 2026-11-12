package com.example.demo.report.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportTextEmbeddingWriterTest {

    @Test
    void writesAPostgresArrayLiteral() {
        assertThat(ReportTextEmbeddingWriter.arrayLiteral(new float[]{0.5f, -0.25f, 1.0E-5f}))
                .isEqualTo("{0.5,-0.25,1.0E-5}");
    }
}
