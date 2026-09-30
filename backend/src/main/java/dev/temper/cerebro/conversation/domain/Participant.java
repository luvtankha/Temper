package dev.temper.cerebro.conversation.domain;

import java.util.*;
import dev.temper.cerebro.common.DomainChecks;

public record Participant(UUID id, UUID conversationId, String userId) {
    public Participant {Objects.requireNonNull(id); Objects.requireNonNull(conversationId); userId = DomainChecks.text(userId, 80, "user id");}
}
