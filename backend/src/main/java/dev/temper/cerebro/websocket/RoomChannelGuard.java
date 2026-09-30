package dev.temper.cerebro.websocket;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.messaging.*;
import org.springframework.messaging.support.*;
import org.springframework.messaging.simp.stomp.*;
import dev.temper.cerebro.conversation.port.ParticipantRepository;

/** Development identity claims, not authentication. Session is limited to its joined room. */
@Component
public class RoomChannelGuard implements ChannelInterceptor {
    private final ParticipantRepository participants;
    public RoomChannelGuard(ParticipantRepository participants) {this.participants=participants;}
    public Message<?> preSend(Message<?> message,MessageChannel channel) {
        var a=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);if(a==null)return message;
        if(a.getCommand()==StompCommand.CONNECT) {
            UUID room=UUID.fromString(a.getFirstNativeHeader("conversationId"));String speaker=a.getFirstNativeHeader("speakerId");
            if(participants.findParticipants(room).stream().noneMatch(p->p.userId().equals(speaker)))throw new IllegalArgumentException("Invalid room membership");
            Objects.requireNonNull(a.getSessionAttributes()).put("room",room);a.getSessionAttributes().put("speaker",speaker);
        }
        if(a.getCommand()==StompCommand.SUBSCRIBE||a.getCommand()==StompCommand.SEND) {
            UUID room=(UUID)Objects.requireNonNull(a.getSessionAttributes()).get("room");String d=a.getDestination();
            Set<String> allowed=a.getCommand()==StompCommand.SUBSCRIBE?Set.of("/topic/conversations/"+room+"/messages","/topic/conversations/"+room+"/analysis","/topic/conversations/"+room+"/typing","/topic/conversations/"+room+"/presence","/user/queue/errors"):Set.of("/app/conversations/"+room+"/send","/app/conversations/"+room+"/typing","/app/conversations/"+room+"/presence");
            if(room==null||d==null||!allowed.contains(d))throw new IllegalArgumentException("Destination outside joined room");
        }
        return message;
    }
}
