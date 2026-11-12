package com.example.demo.matching;

import com.example.demo.shared.embedding.TextEmbedder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

@EnabledIfSystemProperty(named = "matching.evaluation.models", matches = ".+")
class TextSimilarityEvaluationTest {

    private static final String PLAIN_TRIGRAM = "SELECT similarity(unaccent(lower(?)), unaccent(lower(?)))";
    private static final String TRANSLITERATED_TRIGRAM =
            "SELECT similarity(unaccent(lower(transliterate_serbian(?))), unaccent(lower(transliterate_serbian(?))))";
    private static final double[] BLENDS = {0, 0.25, 0.5};
    private static final double[] FLOORS = {0, 0.3, 0.4, 0.5};

    record Pair(String group, boolean match, String lost, String found) {
    }

    record Scored(Pair pair, double plainTrigram, double trigram, Map<String, Double> cosine) {
    }

    @Test
    void comparesTrigramWithEmbeddingModels() throws Exception {
        List<Pair> pairs = readPairs();
        Map<String, TextEmbedder> embedders = loadEmbedders();
        List<Scored> scored = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(
                System.getProperty("matching.evaluation.db.url"),
                System.getProperty("matching.evaluation.db.user"),
                System.getProperty("matching.evaluation.db.password"))) {
            for (Pair pair : pairs) {
                Map<String, Double> cosine = new LinkedHashMap<>();
                embedders.forEach((name, embedder) -> cosine.put(name,
                        TextEmbedder.cosineSimilarity(embedder.embed(pair.lost()), embedder.embed(pair.found()))));
                scored.add(new Scored(pair,
                        similarity(connection, PLAIN_TRIGRAM, pair),
                        similarity(connection, TRANSLITERATED_TRIGRAM, pair),
                        cosine));
            }
        }
        report(scored, embedders.keySet());
        for (TextEmbedder embedder : embedders.values()) {
            embedder.close();
        }
    }

    private static void report(List<Scored> scored, Iterable<String> models) {
        print("trigram danas", scored, Scored::plainTrigram);
        print("trigram + transliteracija", scored, Scored::trigram);
        for (String model : models) {
            for (double floor : FLOORS) {
                for (double blend : BLENDS) {
                    String label = String.format("%s, prag %.2f, alfa %.2f", model, floor, blend);
                    print(label, scored, value -> blend * value.trigram()
                            + (1 - blend) * Math.max(0, (value.cosine().get(model) - floor) / (1 - floor)));
                }
            }
        }
    }

    private static void print(String label, List<Scored> scored, ToDoubleFunction<Scored> score) {
        StringBuilder line = new StringBuilder(String.format("%-34s AUC %.3f  F1 %.3f @ %.2f |", label,
                areaUnderCurve(scored, score), bestF1(scored, score)[0], bestF1(scored, score)[1]));
        for (String group : List.of("latinica", "cirilica", "sinonimi", "engleski", "negativ")) {
            double average = scored.stream().filter(value -> value.pair().group().equals(group))
                    .mapToDouble(score).average().orElse(0);
            line.append(String.format(" %s %.2f", group, average));
        }
        System.out.println(line);
    }

    private static double areaUnderCurve(List<Scored> scored, ToDoubleFunction<Scored> score) {
        List<Double> positives = scored.stream().filter(value -> value.pair().match()).map(score::applyAsDouble).toList();
        List<Double> negatives = scored.stream().filter(value -> !value.pair().match()).map(score::applyAsDouble).toList();
        double wins = 0;
        for (double positive : positives) {
            for (double negative : negatives) {
                wins += positive > negative ? 1 : positive == negative ? 0.5 : 0;
            }
        }
        return wins / (positives.size() * negatives.size());
    }

    private static double[] bestF1(List<Scored> scored, ToDoubleFunction<Scored> score) {
        double bestF1 = 0;
        double bestThreshold = 0;
        for (int step = 1; step < 100; step++) {
            double threshold = step / 100.0;
            int truePositives = 0;
            int falsePositives = 0;
            int falseNegatives = 0;
            for (Scored value : scored) {
                boolean predicted = score.applyAsDouble(value) >= threshold;
                if (predicted && value.pair().match()) {
                    truePositives++;
                } else if (predicted) {
                    falsePositives++;
                } else if (value.pair().match()) {
                    falseNegatives++;
                }
            }
            double f1 = truePositives == 0 ? 0 : 2.0 * truePositives / (2.0 * truePositives + falsePositives + falseNegatives);
            if (f1 > bestF1) {
                bestF1 = f1;
                bestThreshold = threshold;
            }
        }
        return new double[]{bestF1, bestThreshold};
    }

    private static double similarity(Connection connection, String sql, Pair pair) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pair.lost());
            statement.setString(2, pair.found());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getDouble(1);
            }
        }
    }

    private static Map<String, TextEmbedder> loadEmbedders() {
        Map<String, TextEmbedder> embedders = new LinkedHashMap<>();
        for (String entry : System.getProperty("matching.evaluation.models").split(",")) {
            String[] parts = entry.split("=", -1);
            Path directory = Path.of(parts[1]);
            String prefix = parts.length > 2 ? parts[2] : "";
            TextEmbedder.load(directory, prefix, 128, 2).ifPresent(embedder -> embedders.put(parts[0], embedder));
        }
        return embedders;
    }

    private static List<Pair> readPairs() throws Exception {
        List<Pair> pairs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                TextSimilarityEvaluationTest.class.getResourceAsStream("/matching/text-pairs.tsv")),
                StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] columns = line.split("\t");
                pairs.add(new Pair(columns[0], "1".equals(columns[1]), columns[2], columns[3]));
            }
        }
        return pairs;
    }
}
