package dev.temper.cerebro.conversation.api;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import dev.temper.cerebro.auth.port.UserRepository;
import dev.temper.cerebro.conversation.domain.Conversation;
import dev.temper.cerebro.conversation.port.*;
import dev.temper.cerebro.chat.port.MessageRepository;
import dev.temper.cerebro.analysis.port.AnalysisRepository;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationReadController {
    private final ConversationRepository conversations;
    private final ParticipantRepository participants;
    private final UserRepository users;
    private final MessageRepository messages;
    private final AnalysisRepository analyses;
    public ConversationReadController(ConversationRepository conversations, ParticipantRepository participants, UserRepository users, MessageRepository messages, AnalysisRepository analyses) {
        this.conversations = conversations; this.participants = participants; this.users = users; this.messages = messages; this.analyses = analyses;
    }
    public record ParticipantView(String id, String name, String variant) {}
    public record ConversationView(UUID id, String title, Instant createdAt, List<ParticipantView> participants, long messageCount, String analysisStatus) {}
    @GetMapping public List<ConversationView> list() {return conversations.findConversations().stream().map(this::view).toList();}
    @GetMapping("/{id}") public ConversationView get(@PathVariable UUID id) {
        return view(conversations.findConversation(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found")));
    }
    private ConversationView view(Conversation conversation) {
        List<ParticipantView> memberViews = participants.findParticipants(conversation.id()).stream().map(participant -> {
            var user = users.findUser(participant.userId()).orElseThrow(() -> new IllegalStateException("participant user missing"));
            return new ParticipantView(user.id(), user.displayName(), user.avatarVariant().name().toLowerCase(Locale.ROOT));
        }).toList();
        return new ConversationView(conversation.id(), conversation.title(), conversation.createdAt(), memberViews, messages.findMessages(conversation.id()).size(), analyses.findConversationAnalysis(conversation.id()).map(a -> a.mode().name()).orElse("NONE"));
    }
}
