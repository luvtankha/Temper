package dev.temper.cerebro.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class SentimentModel implements AutoCloseable {
    public record Spec(String modelId, String modelSha256, String tokenizerSha256, List<String> labels, int maxTokens) {}
    private final OnnxTextClassifier classifier;
    public SentimentModel(@Value("${temper.sentiment.model-dir:}") String directory, ObjectMapper json) throws Exception {
        if (directory.isBlank()) { classifier = null; return; }
        Spec spec;
        try (var stream = SentimentModel.class.getResourceAsStream("/models/sentiment.json")) {
            if (stream == null) throw new IllegalStateException("Missing sentiment model specification");
            spec = json.readValue(stream, Spec.class);
        }
        classifier = new OnnxTextClassifier(Path.of(directory), spec.modelId(), spec.modelSha256(), spec.tokenizerSha256(), spec.labels(), spec.maxTokens());
    }
    public boolean available() { return classifier != null; }
    public ClassificationResult classify(String text) {
        if (classifier == null) throw new IllegalStateException("Sentiment model unavailable");
        return classifier.classify(text);
    }
    @Override public void close() throws Exception { if (classifier != null) classifier.close(); }
}
