package dev.temper.cerebro.conflict.domain;

import java.time.Instant;
import java.util.*;
import dev.temper.cerebro.common.DomainChecks;

public record EscalationEvent(UUID id, UUID conversationId, UUID messageId, long sequence, Kind kind, Instant occurredAt, String explanation) {
    public enum Kind { ESCALATION, PEAK, RECOVERY, MAJOR_SHIFT }
    public EscalationEvent {
        Objects.requireNonNull(id); Objects.requireNonNull(conversationId); Objects.requireNonNull(messageId); Objects.requireNonNull(kind); Objects.requireNonNull(occurredAt);
        sequence = DomainChecks.sequence(sequence); explanation = DomainChecks.text(explanation, 2000, "event explanation");
    }
}
