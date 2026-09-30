package dev.temper.cerebro.conversation.context;
import java.time.Instant;
import java.util.*;
import dev.temper.cerebro.auth.domain.User;

public record ContextWindow(UUID conversationId,Turn current,List<Turn> previous) {
    public record Speaker(String id,String displayName,User.AvatarVariant avatarVariant) {public Speaker{Objects.requireNonNull(id);Objects.requireNonNull(displayName);Objects.requireNonNull(avatarVariant);}}
    public record Turn(UUID messageId,Speaker speaker,String text,long sequence,Instant sentAt) {public Turn{Objects.requireNonNull(messageId);Objects.requireNonNull(speaker);Objects.requireNonNull(text);Objects.requireNonNull(sentAt);if(sequence<1)throw new IllegalArgumentException("Invalid turn sequence");}}
    public ContextWindow {
        Objects.requireNonNull(conversationId);Objects.requireNonNull(current);previous=List.copyOf(previous);
        if(previous.size()>5)throw new IllegalArgumentException("Context exceeds five preceding turns");
        long prior=0;Set<UUID> ids=new HashSet<>();for(Turn turn:previous){if(turn.sequence()<=prior||turn.sequence()>=current.sequence()||turn.messageId().equals(current.messageId())||!ids.add(turn.messageId()))throw new IllegalArgumentException("Context must contain distinct, ordered, preceding turns");prior=turn.sequence();}
    }
}
