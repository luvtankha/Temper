package dev.temper.cerebro.emotion.domain;

import java.time.Instant;
import java.util.*;
import dev.temper.cerebro.common.DomainChecks;

public record EmotionSnapshot(UUID id, UUID conversationId, UUID messageId, String speakerId, Map<String, Double> emotions,
        double valence, double arousal, double sarcasm, Instant createdAt) {
    public EmotionSnapshot {
        Objects.requireNonNull(id); Objects.requireNonNull(conversationId); Objects.requireNonNull(messageId); Objects.requireNonNull(createdAt);
        speakerId = DomainChecks.text(speakerId, 80, "speaker id");
        emotions = DomainChecks.scores(emotions, Set.of("anger", "sadness", "happiness", "frustration", "confusion", "concern", "surprise"));
        valence = DomainChecks.signedScore(valence, "valence"); arousal = DomainChecks.score(arousal, "arousal"); sarcasm = DomainChecks.score(sarcasm, "sarcasm");
    }
}
