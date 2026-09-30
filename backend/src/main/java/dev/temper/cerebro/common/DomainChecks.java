package dev.temper.cerebro.common;

import java.util.*;

public final class DomainChecks {
    private DomainChecks() {}
    public static final Set<String> EMOTIONS = Set.of("frustration", "anger", "sadness", "happiness", "confusion", "concern");
    public static final Set<String> SIGNALS = Set.of("negativeSentiment", "sarcasm", "toxicity", "passiveAggression", "defensiveness", "blame");
    public static String text(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException(field + " must contain 1–" + max + " characters");
        return value;
    }
    public static double score(double value, String field) {
        if (!Double.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException(field + " must be finite and within 0–1");
        return value;
    }
    public static double signedScore(double value, String field) {
        if (!Double.isFinite(value) || value < -1 || value > 1) throw new IllegalArgumentException(field + " must be finite and within −1–1");
        return value;
    }
    public static long sequence(long value) {
        if (value < 1) throw new IllegalArgumentException("sequence must be positive");
        return value;
    }
    public static Map<String, Double> scores(Map<String, Double> values, Set<String> required) {
        Objects.requireNonNull(values, "scores");
        if (!values.keySet().containsAll(required)) throw new IllegalArgumentException("required score channels are missing");
        values.forEach((key, value) -> {text(key, 80, "channel"); score(Objects.requireNonNull(value), key);});
        return Map.copyOf(values);
    }
}
