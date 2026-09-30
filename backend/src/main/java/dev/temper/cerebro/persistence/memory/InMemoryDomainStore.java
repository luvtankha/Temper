package dev.temper.cerebro.persistence.memory;

import java.util.*;
import org.springframework.stereotype.Repository;
import dev.temper.cerebro.auth.domain.User;
import dev.temper.cerebro.auth.port.UserRepository;
import dev.temper.cerebro.conversation.domain.*;
import dev.temper.cerebro.conversation.port.*;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.chat.port.MessageRepository;
import dev.temper.cerebro.analysis.domain.*;
import dev.temper.cerebro.analysis.port.AnalysisRepository;
import dev.temper.cerebro.emotion.domain.EmotionSnapshot;
import dev.temper.cerebro.emotion.port.EmotionSnapshotRepository;
import dev.temper.cerebro.conflict.domain.EscalationEvent;
import dev.temper.cerebro.conflict.port.EscalationEventRepository;

/** One process-local adapter. Domain/application services depend on ports, not this class. */
@Repository
public class InMemoryDomainStore implements UserRepository, ConversationRepository, ParticipantRepository,
        MessageRepository, AnalysisRepository, EmotionSnapshotRepository, EscalationEventRepository {
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<UUID, Conversation> conversations = new LinkedHashMap<>();
    private final Map<UUID, Participant> participants = new LinkedHashMap<>();
    private final Map<UUID, Message> messages = new LinkedHashMap<>();
    private final Map<UUID, MessageAnalysis> messageAnalyses = new LinkedHashMap<>();
    private final Map<UUID, ConversationAnalysis> conversationAnalyses = new LinkedHashMap<>();
    private final Map<UUID, EmotionSnapshot> snapshots = new LinkedHashMap<>();
    private final Map<UUID, EscalationEvent> events = new LinkedHashMap<>();
    public synchronized Optional<User> findUser(String id) {return Optional.ofNullable(users.get(id));}
    public synchronized List<User> findUsers() {return List.copyOf(users.values());}
    public synchronized void saveUser(User user) {users.put(user.id(), user);}
    public synchronized Optional<Conversation> findConversation(UUID id) {return Optional.ofNullable(conversations.get(id));}
    public synchronized List<Conversation> findConversations() {return conversations.values().stream().sorted(Comparator.comparing(Conversation::createdAt).thenComparing(Conversation::id)).toList();}
    public synchronized void saveConversation(Conversation conversation) {conversations.put(conversation.id(), conversation);}
    public synchronized List<Participant> findParticipants(UUID conversationId) {return participants.values().stream().filter(p -> p.conversationId().equals(conversationId)).sorted(Comparator.comparing(Participant::userId)).toList();}
    public synchronized void saveParticipant(Participant participant) {
        requireConversation(participant.conversationId());
        if (!users.containsKey(participant.userId())) throw new IllegalArgumentException("participant user does not exist");
        if (participants.values().stream().anyMatch(p -> p.conversationId().equals(participant.conversationId()) && p.userId().equals(participant.userId()) && !p.id().equals(participant.id()))) throw new IllegalArgumentException("user already participates in conversation");
        if (participants.containsKey(participant.id()) && !participants.get(participant.id()).equals(participant)) throw new IllegalArgumentException("participant identity is immutable");
        participants.put(participant.id(), participant);
    }
    public synchronized Optional<Message> findMessage(UUID id) {return Optional.ofNullable(messages.get(id));}
    public synchronized List<Message> findMessages(UUID conversationId) {return messages.values().stream().filter(m -> m.conversationId().equals(conversationId)).sorted(Comparator.comparingLong(Message::sequence)).toList();}
    public synchronized void saveMessage(Message message) {
        requireConversation(message.conversationId());
        if (findParticipants(message.conversationId()).stream().noneMatch(p -> p.userId().equals(message.speakerId()))) throw new IllegalArgumentException("speaker is not a conversation participant");
        Message existing = messages.get(message.id());
        if (existing != null) {if (existing.equals(message)) return; throw new IllegalArgumentException("message identity is immutable");}
        if (messages.values().stream().anyMatch(m -> m.conversationId().equals(message.conversationId()) && m.sequence() == message.sequence())) throw new IllegalArgumentException("message sequence already exists");
        messages.put(message.id(), message);
    }
    public synchronized Optional<MessageAnalysis> findMessageAnalysis(UUID messageId) {return Optional.ofNullable(messageAnalyses.get(messageId));}
    public synchronized void saveMessageAnalysis(MessageAnalysis analysis) {
        Message message = requireMessage(analysis.messageId());
        if (!message.speakerId().equals(analysis.speakerId()) || message.sequence() != analysis.sequence()) throw new IllegalArgumentException("analysis must identify the analyzed message");
        for (UUID id : analysis.contextMessageIds()) {
            Message previous = requireMessage(id);
            if (!previous.conversationId().equals(message.conversationId()) || previous.sequence() >= message.sequence()) throw new IllegalArgumentException("analysis context must contain preceding turns from this conversation");
        }
        messageAnalyses.put(analysis.messageId(), analysis);
    }
    public synchronized Optional<ConversationAnalysis> findConversationAnalysis(UUID id) {return Optional.ofNullable(conversationAnalyses.get(id));}
    public synchronized void saveConversationAnalysis(ConversationAnalysis analysis) {
        requireConversation(analysis.conversationId());
        if (analysis.escalationStartMessageId() != null) requireMessageInConversation(analysis.escalationStartMessageId(), analysis.conversationId());
        if (analysis.peakTensionMessageId() != null) requireMessageInConversation(analysis.peakTensionMessageId(), analysis.conversationId());
        analysis.events().forEach(event -> {
            Message message = requireMessageInConversation(event.messageId(), analysis.conversationId());
            if (message.sequence() != event.sequence()) throw new IllegalArgumentException("event sequence must match its message");
        });
        conversationAnalyses.put(analysis.conversationId(), analysis);
    }
    public synchronized List<EmotionSnapshot> findEmotionSnapshots(UUID conversationId) {return snapshots.values().stream().filter(s -> s.conversationId().equals(conversationId)).sorted(Comparator.comparing(EmotionSnapshot::createdAt).thenComparing(EmotionSnapshot::id)).toList();}
    public synchronized void saveEmotionSnapshot(EmotionSnapshot snapshot) {
        Message message = requireMessageInConversation(snapshot.messageId(), snapshot.conversationId());
        if (!message.speakerId().equals(snapshot.speakerId())) throw new IllegalArgumentException("snapshot must identify the message speaker");
        snapshots.put(snapshot.id(), snapshot);
    }
    public synchronized List<EscalationEvent> findEscalationEvents(UUID conversationId) {return events.values().stream().filter(e -> e.conversationId().equals(conversationId)).sorted(Comparator.comparingLong(EscalationEvent::sequence).thenComparing(EscalationEvent::id)).toList();}
    public synchronized void saveEscalationEvent(EscalationEvent event) {
        Message message = requireMessageInConversation(event.messageId(), event.conversationId());
        if (message.sequence() != event.sequence()) throw new IllegalArgumentException("event sequence must match its message");
        events.put(event.id(), event);
    }
    private void requireConversation(UUID id) {if (!conversations.containsKey(id)) throw new IllegalArgumentException("conversation does not exist");}
    private Message requireMessage(UUID id) {return Optional.ofNullable(messages.get(id)).orElseThrow(() -> new IllegalArgumentException("message does not exist"));}
    private Message requireMessageInConversation(UUID id, UUID conversationId) {
        Message message = requireMessage(id);
        if (!message.conversationId().equals(conversationId)) throw new IllegalArgumentException("message belongs to a different conversation");
        return message;
    }
}
