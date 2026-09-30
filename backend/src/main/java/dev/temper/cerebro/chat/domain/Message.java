package dev.temper.cerebro.chat.domain;

import java.time.Instant;
import java.util.*;
import dev.temper.cerebro.common.DomainChecks;

public record Message(UUID id, UUID conversationId, String speakerId, String text, Instant sentAt, long sequence) {
    public Message {
        Objects.requireNonNull(id); Objects.requireNonNull(conversationId); Objects.requireNonNull(sentAt);
        speakerId = DomainChecks.text(speakerId, 80, "speaker id"); text = DomainChecks.text(text, 2000, "message");
        sequence = DomainChecks.sequence(sequence);
    }
}
