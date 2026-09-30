package dev.temper.cerebro;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;
import dev.temper.cerebro.auth.domain.User;
import dev.temper.cerebro.conversation.domain.*;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.analysis.domain.*;
import dev.temper.cerebro.emotion.domain.EmotionSnapshot;
import dev.temper.cerebro.conflict.domain.EscalationEvent;
import dev.temper.cerebro.persistence.memory.InMemoryDomainStore;
import dev.temper.cerebro.common.DomainChecks;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryDomainStoreTest {
    private InMemoryDomainStore store;
    private UUID conversationId;
    private final Instant now = Instant.parse("2026-10-01T08:00:00Z");
    @BeforeEach void setup() {
        store = new InMemoryDomainStore();
        store.saveUser(new User("alex", "Alex", User.AvatarVariant.MALE));
        store.saveUser(new User("nova", "Nova", User.AvatarVariant.FEMALE));
        conversationId = conversation();
    }
    private UUID conversation() {
        UUID id = UUID.randomUUID(); store.saveConversation(new Conversation(id, "Fictional test", now));
        store.saveParticipant(new Participant(UUID.randomUUID(), id, "alex")); store.saveParticipant(new Participant(UUID.randomUUID(), id, "nova")); return id;
    }
    private Message message(UUID conversation, long sequence) {return new Message(UUID.randomUUID(), conversation, "alex", "Fictional turn " + sequence, now.plusSeconds(sequence), sequence);}
    private Map<String, Double> scores(Set<String> keys) {return keys.stream().collect(Collectors.toMap(key -> key, key -> .25));}
    private MessageAnalysis analysis(Message message, List<UUID> context) {
        return new MessageAnalysis(message.id(), message.speakerId(), message.sequence(), AnalysisMode.MOCK, scores(DomainChecks.EMOTIONS), scores(DomainChecks.SIGNALS), -.3, .2, context, "Mock test only", List.of(new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.MOCK, "Test fixture", "No inference")), now);
    }
    @Test void membershipIdentityAndSequenceViolationsDoNotMutateStoredMessages() {
        assertThrows(IllegalArgumentException.class, () -> store.saveParticipant(new Participant(UUID.randomUUID(), conversationId, "missing")));
        assertThrows(IllegalArgumentException.class, () -> store.saveParticipant(new Participant(UUID.randomUUID(), conversationId, "alex")));
        assertThrows(IllegalArgumentException.class, () -> store.saveMessage(new Message(UUID.randomUUID(), conversationId, "outsider", "Not a member", now, 1)));
        Message second = message(conversationId, 2), first = message(conversationId, 1);
        store.saveMessage(second); store.saveMessage(first); store.saveMessage(first);
        assertEquals(List.of(first, second), store.findMessages(conversationId));
        assertThrows(IllegalArgumentException.class, () -> store.saveMessage(message(conversationId, 2)));
        assertThrows(IllegalArgumentException.class, () -> store.saveMessage(new Message(first.id(), conversationId, "alex", "Changed immutable message", now, 1)));
        assertEquals(first, store.findMessage(first.id()).orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> store.findMessages(conversationId).clear());
    }
    @Test void analysisCannotUseForeignConversationOrFutureContext() {
        Message first = message(conversationId, 1), second = message(conversationId, 2), other = message(conversation(), 1);
        for (Message message : List.of(first, second, other)) store.saveMessage(message);
        assertThrows(IllegalArgumentException.class, () -> store.saveMessageAnalysis(analysis(second, List.of(other.id()))));
        assertThrows(IllegalArgumentException.class, () -> store.saveMessageAnalysis(analysis(first, List.of(second.id()))));
        assertTrue(store.findMessageAnalysis(second.id()).isEmpty());
        MessageAnalysis valid = analysis(second, List.of(first.id())); store.saveMessageAnalysis(valid);
        assertEquals(valid, store.findMessageAnalysis(second.id()).orElseThrow());
    }
    @Test void scoreMapsAndContextAreImmutableSnapshotsNotCallerOwnedCollections() {
        Message message = message(conversationId, 1); Map<String, Double> emotionScores = scores(DomainChecks.EMOTIONS); List<UUID> context = new ArrayList<>();
        MessageAnalysis analysis = new MessageAnalysis(message.id(), "alex", 1, AnalysisMode.MOCK, emotionScores, scores(DomainChecks.SIGNALS), 0, 0, context, "Mock only", List.of(), now);
        emotionScores.put("anger", .9); context.add(UUID.randomUUID());
        assertEquals(.25, analysis.emotions().get("anger")); assertTrue(analysis.contextMessageIds().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> analysis.emotions().put("anger", .7));
        assertThrows(IllegalArgumentException.class, () -> new MessageAnalysis(message.id(), "alex", 1, AnalysisMode.MOCK, emotionScores, scores(DomainChecks.SIGNALS), 0, 0, List.of(message.id()), "Mock", List.of(), now));
    }
    @Test void snapshotsAndEventsStayWithTheirMessageAndSpeaker() {
        Message message = message(conversationId, 2); store.saveMessage(message);
        Map<String, Double> emotions = scores(Set.of("anger", "sadness", "happiness", "frustration", "confusion", "concern", "surprise"));
        EmotionSnapshot snapshot = new EmotionSnapshot(UUID.randomUUID(), conversationId, message.id(), "alex", emotions, -.5, .6, .1, now); store.saveEmotionSnapshot(snapshot);
        assertThrows(IllegalArgumentException.class, () -> store.saveEmotionSnapshot(new EmotionSnapshot(UUID.randomUUID(), conversationId, message.id(), "nova", emotions, 0, 0, 0, now)));
        assertEquals(List.of(snapshot), store.findEmotionSnapshots(conversationId));
        EscalationEvent invalid = new EscalationEvent(UUID.randomUUID(), conversationId, message.id(), 1, EscalationEvent.Kind.ESCALATION, now, "Mock marker");
        assertThrows(IllegalArgumentException.class, () -> store.saveEscalationEvent(invalid));
        assertThrows(IllegalArgumentException.class, () -> store.saveConversationAnalysis(new ConversationAnalysis(conversationId, AnalysisMode.MOCK, .2, -.3, .4, ConversationAnalysis.Direction.ESCALATING, message.id(), message.id(), List.of(invalid), now)));
        EscalationEvent valid = new EscalationEvent(UUID.randomUUID(), conversationId, message.id(), 2, EscalationEvent.Kind.ESCALATION, now, "Mock marker"); store.saveEscalationEvent(valid);
        assertEquals(List.of(valid), store.findEscalationEvents(conversationId));
    }
    @Test void invalidScoresAreRejectedInsteadOfSilentlyClamped() {
        assertThrows(IllegalArgumentException.class, () -> DomainChecks.score(Double.NaN, "signal"));
        assertThrows(IllegalArgumentException.class, () -> DomainChecks.score(-.1, "signal"));
        assertThrows(IllegalArgumentException.class, () -> DomainChecks.signedScore(-2, "sentiment"));
        assertThrows(IllegalArgumentException.class, () -> DomainChecks.scores(Map.of("anger", .2), DomainChecks.EMOTIONS));
        assertThrows(IllegalArgumentException.class, () -> new Message(UUID.randomUUID(), conversationId, "alex", " ", now, 1));
    }
    @Test void concurrentUniqueWritesAreNotLostAndRemainSequenceOrdered() throws Exception {
        List<Callable<Void>> tasks = LongStream.rangeClosed(1, 100).mapToObj(sequence -> (Callable<Void>) () -> {store.saveMessage(message(conversationId, sequence)); return null;}).toList();
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {for (Future<Void> result : executor.invokeAll(tasks)) result.get();}
        assertEquals(LongStream.rangeClosed(1, 100).boxed().toList(), store.findMessages(conversationId).stream().map(Message::sequence).toList());
    }
}
