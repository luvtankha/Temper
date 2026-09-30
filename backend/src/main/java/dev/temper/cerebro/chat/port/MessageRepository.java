package dev.temper.cerebro.chat.port;
import java.util.*;
import dev.temper.cerebro.chat.domain.Message;
public interface MessageRepository {Optional<Message> findMessage(UUID id); List<Message> findMessages(UUID conversationId); void saveMessage(Message message);}
