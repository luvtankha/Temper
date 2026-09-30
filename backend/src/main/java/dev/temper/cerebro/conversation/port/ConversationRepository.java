package dev.temper.cerebro.conversation.port;
import java.util.*;
import dev.temper.cerebro.conversation.domain.Conversation;
public interface ConversationRepository {Optional<Conversation> findConversation(UUID id); List<Conversation> findConversations(); void saveConversation(Conversation conversation);}
