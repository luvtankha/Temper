package dev.temper.cerebro.analysis.domain;

import java.time.Instant;
import java.util.*;
import dev.temper.cerebro.conflict.domain.EscalationEvent;
import dev.temper.cerebro.common.DomainChecks;

public record ConversationAnalysis(UUID conversationId, AnalysisMode mode, double conflictScore, double sentiment,
        double emotionalIntensity, Direction direction, UUID escalationStartMessageId, UUID peakTensionMessageId,
        List<EscalationEvent> events, Instant analyzedAt) {
    public enum Direction { STEADY, ESCALATING, RECOVERING }
    public ConversationAnalysis {
        Objects.requireNonNull(conversationId); Objects.requireNonNull(mode); Objects.requireNonNull(direction); Objects.requireNonNull(analyzedAt);
        conflictScore = DomainChecks.score(conflictScore, "conflict"); sentiment = DomainChecks.signedScore(sentiment, "sentiment"); emotionalIntensity = DomainChecks.score(emotionalIntensity, "intensity");
        events = List.copyOf(events);
        if (events.stream().anyMatch(event -> !event.conversationId().equals(conversationId))) throw new IllegalArgumentException("events must belong to the analyzed conversation");
    }
}
