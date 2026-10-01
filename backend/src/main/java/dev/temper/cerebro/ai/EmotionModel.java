package dev.temper.cerebro.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class EmotionModel implements AutoCloseable {
    private final OnnxTextClassifier classifier;
    public EmotionModel(@Value("${temper.emotion.model-dir:}") String directory, ObjectMapper json) throws Exception {
        if (directory.isBlank()) { classifier=null; return; }
        SentimentModel.Spec spec;
        try (var stream=EmotionModel.class.getResourceAsStream("/models/emotion.json")) {
            if (stream==null) throw new IllegalStateException("Missing emotion specification");
            spec=json.readValue(stream, SentimentModel.Spec.class);
        }
        classifier=new OnnxTextClassifier(Path.of(directory),spec.modelId(),spec.modelSha256(),spec.tokenizerSha256(),spec.labels(),spec.maxTokens(),true);
    }
    public boolean available() {return classifier!=null;}
    public ClassificationResult classify(String text) {
        if(classifier==null)throw new IllegalStateException("Emotion model unavailable");
        return classifier.classify(text);
    }
    /** Semantic proxies are deliberately named and inspectable, never disguised as direct trained labels. */
    public static Map<String,Double> map(Map<String,Double> raw) {
        return Map.of("anger",raw.get("anger"),"frustration",raw.get("annoyance"),"sadness",raw.get("sadness"),
            "happiness",raw.get("joy"),"confusion",raw.get("confusion"),"concern",Math.max(raw.get("caring"),Math.max(raw.get("fear"),raw.get("nervousness"))),
            "surprise",raw.get("surprise"),"neutral",raw.get("neutral"));
    }
    @Override public void close() throws Exception {if(classifier!=null)classifier.close();}
}
