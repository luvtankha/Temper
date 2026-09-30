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
import dev.temper.cerebro.conversation.service.ConversationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationReadController {
    private final ConversationRepository conversations;
    private final ParticipantRepository participants;
    private final UserRepository users;
    private final MessageRepository messages;
    private final AnalysisRepository analyses;
    private final ConversationService service;
    public ConversationReadController(ConversationRepository conversations, ParticipantRepository participants, UserRepository users, MessageRepository messages, AnalysisRepository analyses, ConversationService service) {
        this.conversations = conversations; this.participants = participants; this.users = users; this.messages = messages; this.analyses = analyses;
        this.service=service;
    }
    public record ParticipantView(String id, String name, String variant) {}
    public record ConversationView(UUID id, String title, Instant createdAt, List<ParticipantView> participants, long messageCount, String analysisStatus) {}
    @GetMapping public List<ConversationView> list() {return conversations.findConversations().stream().map(this::view).toList();}
    public record CreateRequest(@NotBlank @Size(max=160) String title, @NotNull @Size(min=2,max=20) List<@NotBlank String> participantIds) {}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ConversationView create(@Valid @RequestBody CreateRequest request) {return view(service.create(request.title().trim(),request.participantIds()));}
    @GetMapping("/{id}") public ConversationView get(@PathVariable UUID id) {
        return view(conversations.findConversation(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found")));
    }
    private ConversationView view(Conversation conversation) {
        List<ParticipantView> memberViews = participants.findParticipants(conversation.id()).stream().map(participant -> {
            var user = users.findUser(participant.userId()).orElseThrow(() -> new IllegalStateException("participant user missing"));
            return new ParticipantView(user.id(), user.displayName(), user.avatarVariant().name().toLowerCase(Locale.ROOT));
        }).toList();
        var turns=messages.findMessages(conversation.id());
        String analysisStatus=analyses.findConversationAnalysis(conversation.id()).map(a->a.mode().name()).orElseGet(()->turns.stream().map(m->analyses.findMessageAnalysis(m.id())).flatMap(Optional::stream).map(a->a.mode().name()).findFirst().orElse("NONE"));
        return new ConversationView(conversation.id(), conversation.title(), conversation.createdAt(), memberViews, turns.size(), analysisStatus);
    }
}
