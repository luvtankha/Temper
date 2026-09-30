package dev.temper.cerebro.websocket;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import dev.temper.cerebro.conversation.service.ConversationService;

@Controller
public class ChatSocketController {
    private final ConversationService conversations;private final RoomPresence presence;private final SimpMessagingTemplate broker;
    public ChatSocketController(ConversationService conversations,RoomPresence presence,SimpMessagingTemplate broker) {this.conversations=conversations;this.presence=presence;this.broker=broker;}
    public record Send(UUID requestId,String text) {}
    public record Typing(boolean typing) {}
    public record Error(String code,String message,UUID requestId) {}
    private static class SendFailure extends RuntimeException {final UUID requestId;SendFailure(UUID id){super("Message rejected");requestId=id;}}
    private String speaker(SimpMessageHeaderAccessor a,UUID room) {var attrs=Objects.requireNonNull(a.getSessionAttributes());if(!room.equals(attrs.get("room")))throw new IllegalArgumentException("Wrong room");return (String)attrs.get("speaker");}
    @MessageMapping("/conversations/{room}/send")
    public void send(@DestinationVariable UUID room,@Payload Send input,SimpMessageHeaderAccessor headers) {
        try {Objects.requireNonNull(input.requestId());conversations.send(room,speaker(headers,room),input.text()==null?null:input.text().trim(),input.requestId());}
        catch(RuntimeException rejected){throw new SendFailure(input.requestId());}
    }
    @MessageMapping("/conversations/{room}/typing")
    public void typing(@DestinationVariable UUID room,@Payload Typing input,SimpMessageHeaderAccessor headers) {broker.convertAndSend("/topic/conversations/"+room+"/typing",Map.of("speakerId",speaker(headers,room),"typing",input.typing()));}
    @MessageMapping("/conversations/{room}/presence")
    public void presence(@DestinationVariable UUID room,SimpMessageHeaderAccessor headers) {presence.join(headers.getSessionId(),room,speaker(headers,room));}
    @MessageExceptionHandler @SendToUser(value="/queue/errors",broadcast=false)
    public Error error(Exception error) {return new Error("INVALID_MESSAGE","Message was rejected. Check length and room membership.",error instanceof SendFailure failure?failure.requestId:null);}
}
