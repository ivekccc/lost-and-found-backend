package com.example.demo.shared.embedding;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class TextEmbedder implements AutoCloseable {

    private static final String MODEL_FILE = "model.onnx";
    private static final String TOKENIZER_FILE = "tokenizer.json";
    private static final String INPUT_IDS = "input_ids";
    private static final String ATTENTION_MASK = "attention_mask";
    private static final String TOKEN_TYPE_IDS = "token_type_ids";

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;
    private final Set<String> inputNames;
    private final String textPrefix;

    private TextEmbedder(OrtEnvironment environment, OrtSession session, HuggingFaceTokenizer tokenizer,
                         String textPrefix) throws OrtException {
        this.environment = environment;
        this.session = session;
        this.tokenizer = tokenizer;
        this.inputNames = session.getInputNames();
        this.textPrefix = textPrefix == null ? "" : textPrefix;
    }

    public static Optional<TextEmbedder> load(Path modelDirectory, String textPrefix, int maxTokens, int threads) {
        Path modelFile = modelDirectory.resolve(MODEL_FILE);
        Path tokenizerFile = modelDirectory.resolve(TOKENIZER_FILE);
        if (!Files.isRegularFile(modelFile) || !Files.isRegularFile(tokenizerFile)) {
            log.warn("Text embedding model not found in {}, matching falls back to trigram similarity",
                    modelDirectory);
            return Optional.empty();
        }
        OrtSession session = null;
        try {
            OrtEnvironment environment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setIntraOpNumThreads(threads);
            options.setInterOpNumThreads(1);
            session = environment.createSession(modelFile.toString(), options);
            HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile, Map.of(
                    "maxLength", String.valueOf(maxTokens),
                    "truncation", "true",
                    "padding", "false"));
            return Optional.of(new TextEmbedder(environment, session, tokenizer, textPrefix));
        } catch (Exception | LinkageError exception) {
            log.error("Text embedding model in {} could not be loaded, matching falls back to trigram similarity",
                    modelDirectory, exception);
            closeQuietly(session);
            return Optional.empty();
        }
    }

    private static void closeQuietly(OrtSession session) {
        if (session == null) {
            return;
        }
        try {
            session.close();
        } catch (OrtException exception) {
            log.debug("Closing a half-loaded embedding session failed", exception);
        }
    }

    public float[] embed(String text) {
        Encoding encoding = tokenizer.encode(textPrefix + text);
        long[] ids = encoding.getIds();
        long[] mask = encoding.getAttentionMask();
        long[][] shape = {ids};
        Map<String, OnnxTensor> inputs = new HashMap<>();
        try {
            inputs.put(INPUT_IDS, OnnxTensor.createTensor(environment, shape));
            inputs.put(ATTENTION_MASK, OnnxTensor.createTensor(environment, new long[][]{mask}));
            if (inputNames.contains(TOKEN_TYPE_IDS)) {
                inputs.put(TOKEN_TYPE_IDS, OnnxTensor.createTensor(environment, new long[][]{new long[ids.length]}));
            }
            try (OrtSession.Result result = session.run(inputs)) {
                float[][][] hiddenStates = (float[][][]) result.get(0).getValue();
                return normalize(meanPool(hiddenStates[0], mask));
            }
        } catch (OrtException exception) {
            throw new IllegalStateException("Text embedding failed", exception);
        } finally {
            inputs.values().forEach(OnnxTensor::close);
        }
    }

    public static double cosineSimilarity(float[] first, float[] second) {
        if (first == null || second == null || first.length != second.length) {
            return 0;
        }
        double dot = 0;
        for (int index = 0; index < first.length; index++) {
            dot += first[index] * second[index];
        }
        return Math.max(0, Math.min(1, dot));
    }

    private static float[] meanPool(float[][] tokenVectors, long[] mask) {
        int dimensions = tokenVectors[0].length;
        float[] pooled = new float[dimensions];
        int counted = 0;
        for (int token = 0; token < tokenVectors.length; token++) {
            if (mask[token] == 0) {
                continue;
            }
            counted++;
            for (int dimension = 0; dimension < dimensions; dimension++) {
                pooled[dimension] += tokenVectors[token][dimension];
            }
        }
        if (counted > 0) {
            for (int dimension = 0; dimension < dimensions; dimension++) {
                pooled[dimension] /= counted;
            }
        }
        return pooled;
    }

    private static float[] normalize(float[] vector) {
        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) {
            return vector;
        }
        for (int index = 0; index < vector.length; index++) {
            vector[index] = (float) (vector[index] / norm);
        }
        return vector;
    }

    @Override
    public void close() throws OrtException {
        tokenizer.close();
        session.close();
    }
}
