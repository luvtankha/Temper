package dev.temper.cerebro.ai;

import java.util.Map;

public record ClassificationResult(String modelId, String source, Map<String, Double> probabilities,
                                   int tokenCount, long inferenceMillis) {
    public ClassificationResult { probabilities = Map.copyOf(probabilities); }
}
