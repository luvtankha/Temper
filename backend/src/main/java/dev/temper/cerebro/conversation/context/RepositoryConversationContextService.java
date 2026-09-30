package dev.temper.cerebro.conversation.context;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.chat.port.MessageRepository;
import dev.temper.cerebro.auth.port.UserRepository;
import dev.temper.cerebro.conversation.port.ConversationRepository;

@Service
public class RepositoryConversationContextService implements ConversationContextService {
    private final MessageRepository messages;private final UserRepository users;private final ConversationRepository conversations;private final int windowSize;
    public RepositoryConversationContextService(MessageRepository messages,UserRepository users,ConversationRepository conversations,@Value("${temper.context.previous-turns:5}") int windowSize) {
        if(windowSize<3||windowSize>5)throw new IllegalArgumentException("Context previous-turns must be between 3 and 5");this.messages=messages;this.users=users;this.conversations=conversations;this.windowSize=windowSize;
    }
    public ContextWindow buildContext(UUID conversationId,UUID messageId) {
        if(conversations.findConversation(conversationId).isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Conversation not found");
        var current=messages.findMessage(messageId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Message not found"));
        if(!current.conversationId().equals(conversationId))throw new IllegalArgumentException("Context message belongs to another conversation");
        var preceding=messages.findMessages(conversationId).stream().filter(m->m.sequence()<current.sequence()).sorted(Comparator.comparingLong(Message::sequence)).toList();
        return new ContextWindow(conversationId,turn(current),preceding.subList(Math.max(0,preceding.size()-windowSize),preceding.size()).stream().map(this::turn).toList());
    }
    private ContextWindow.Turn turn(Message message) {
        var user=users.findUser(message.speakerId()).orElseThrow(()->new IllegalStateException("Context speaker missing"));
        return new ContextWindow.Turn(message.id(),new ContextWindow.Speaker(user.id(),user.displayName(),user.avatarVariant()),message.text(),message.sequence(),message.sentAt());
    }
}
