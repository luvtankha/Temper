package dev.temper.cerebro.analysis.domain;

import java.time.Instant;
import java.util.*;
import dev.temper.cerebro.common.DomainChecks;

public record MessageAnalysis(UUID messageId, String speakerId, long sequence, AnalysisMode mode,
        Map<String, Double> emotions, Map<String, Double> signals, double sentiment, double conflict,
        List<UUID> contextMessageIds, String explanation, List<Evidence> evidence, Instant analyzedAt) {
    public enum EvidenceSource { MOCK, MODEL, HEURISTIC }
    public record Evidence(EvidenceSource source, String label, String description) {
        public Evidence {Objects.requireNonNull(source); label = DomainChecks.text(label, 160, "evidence label"); description = DomainChecks.text(description, 4000, "evidence description");}
    }
    public MessageAnalysis {
        Objects.requireNonNull(messageId); Objects.requireNonNull(mode); Objects.requireNonNull(analyzedAt);
        speakerId = DomainChecks.text(speakerId, 80, "speaker id"); sequence = DomainChecks.sequence(sequence);
        emotions = DomainChecks.scores(emotions, DomainChecks.EMOTIONS); signals = DomainChecks.scores(signals, DomainChecks.SIGNALS);
        sentiment = DomainChecks.signedScore(sentiment, "sentiment"); conflict = DomainChecks.score(conflict, "conflict");
        contextMessageIds = List.copyOf(contextMessageIds); evidence = List.copyOf(evidence);
        if (contextMessageIds.contains(messageId)) throw new IllegalArgumentException("preceding context cannot include the current message");
        explanation = DomainChecks.text(explanation, 4000, "explanation");
    }
}
