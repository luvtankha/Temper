package dev.temper.cerebro.conversation.domain;

import java.time.Instant;
import java.util.*;
import dev.temper.cerebro.common.DomainChecks;

public record Conversation(UUID id, String title, Instant createdAt) {
    public Conversation {Objects.requireNonNull(id); title = DomainChecks.text(title, 160, "title"); Objects.requireNonNull(createdAt);}
}
