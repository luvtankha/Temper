package dev.temper.cerebro.websocket;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import dev.temper.cerebro.chat.domain.Message;

@Component
public class MessageBroadcast {
    public record Delivered(Message message,UUID requestId) {}
    private final SimpMessagingTemplate broker;
    public MessageBroadcast(SimpMessagingTemplate broker) {this.broker=broker;}
    @EventListener public void delivered(Delivered event) {broker.convertAndSend("/topic/conversations/"+event.message().conversationId()+"/messages",event);}
}
