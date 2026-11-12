package com.example.demo.shared.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Slf4j
public class TextEmbeddings implements DisposableBean {

    private final TextEmbedder embedder;
    private final String modelName;
    private final ReadWriteLock lifecycle = new ReentrantReadWriteLock();
    private boolean closed;

    public TextEmbeddings(@Value("${app.embedding.model-dir:}") String modelDirectory,
                          @Value("${app.embedding.model-name}") String modelName,
                          @Value("${app.embedding.text-prefix:}") String textPrefix,
                          @Value("${app.embedding.max-tokens:128}") int maxTokens,
                          @Value("${app.embedding.threads:1}") int threads) {
        this.modelName = modelName;
        this.embedder = modelDirectory.isBlank()
                ? null
                : TextEmbedder.load(Path.of(modelDirectory), textPrefix, maxTokens, threads).orElse(null);
        if (embedder != null) {
            log.info("Text embedding model {} loaded from {}", modelName, modelDirectory);
        }
    }

    public boolean isEnabled() {
        return embedder != null;
    }

    public String modelName() {
        return modelName;
    }

    public Optional<float[]> embed(String text) {
        if (embedder == null || text == null || text.isBlank()) {
            return Optional.empty();
        }
        lifecycle.readLock().lock();
        try {
            if (closed) {
                return Optional.empty();
            }
            return Optional.of(embedder.embed(text));
        } catch (RuntimeException exception) {
            log.warn("Text embedding failed, the pair falls back to trigram similarity", exception);
            return Optional.empty();
        } finally {
            lifecycle.readLock().unlock();
        }
    }

    @Override
    public void destroy() throws Exception {
        if (embedder == null) {
            return;
        }
        lifecycle.writeLock().lock();
        try {
            closed = true;
            embedder.close();
        } finally {
            lifecycle.writeLock().unlock();
        }
    }
}
