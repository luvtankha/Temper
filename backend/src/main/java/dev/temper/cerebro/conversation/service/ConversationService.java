package dev.temper.cerebro.conversation.service;

import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import dev.temper.cerebro.auth.port.UserRepository;
import dev.temper.cerebro.conversation.domain.*;
import dev.temper.cerebro.conversation.port.*;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.chat.port.MessageRepository;

@Service
public class ConversationService {
    private final ConversationRepository conversations;
    private final ParticipantRepository participants;
    private final UserRepository users;
    private final MessageRepository messages;
    private final Clock clock;
    public ConversationService(ConversationRepository conversations, ParticipantRepository participants, UserRepository users, MessageRepository messages, Clock clock) {
        this.conversations=conversations;this.participants=participants;this.users=users;this.messages=messages;this.clock=clock;
    }
    public Conversation require(UUID id) {return conversations.findConversation(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Conversation not found"));}
    public synchronized Conversation create(String title, List<String> memberIds) {
        if(memberIds==null||memberIds.size()<2||memberIds.size()>20||new HashSet<>(memberIds).size()!=memberIds.size()) throw new IllegalArgumentException("Choose 2–20 distinct participants");
        for(String member:memberIds) if(member==null||users.findUser(member).isEmpty()) throw new IllegalArgumentException("Participant does not exist");
        var conversation=new Conversation(UUID.randomUUID(),title,clock.instant());
        conversations.saveConversation(conversation);
        memberIds.forEach(member->participants.saveParticipant(new Participant(UUID.randomUUID(),conversation.id(),member)));
        return conversation;
    }
    public List<Message> messages(UUID id) {require(id);return messages.findMessages(id);}
    /** Sequence allocation and save form one process-local critical section. */
    public synchronized Message send(UUID id,String speaker,String text) {
        require(id);
        if(participants.findParticipants(id).stream().noneMatch(p->p.userId().equals(speaker))) throw new IllegalArgumentException("Speaker is not a participant");
        long next=messages.findMessages(id).stream().mapToLong(Message::sequence).max().orElse(0)+1;
        var message=new Message(UUID.randomUUID(),id,speaker,text,clock.instant(),next);
        messages.saveMessage(message);return message;
    }
}
