package dev.temper.cerebro.websocket;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Component
public class RoomPresence {
    record Member(UUID room,String speaker) {}
    private final Map<String,Member> sessions=new HashMap<>();private final SimpMessagingTemplate broker;
    public RoomPresence(SimpMessagingTemplate broker) {this.broker=broker;}
    public synchronized void join(String session,UUID room,String speaker) {sessions.put(session,new Member(room,speaker));publish(room);}
    public synchronized List<String> online(UUID room) {return sessions.values().stream().filter(m->m.room().equals(room)).map(Member::speaker).distinct().sorted().toList();}
    public synchronized void publish(UUID room) {broker.convertAndSend("/topic/conversations/"+room+"/presence",Map.of("conversationId",room,"onlineParticipantIds",online(room)));}
    @EventListener public synchronized void disconnected(SessionDisconnectEvent event) {var member=sessions.remove(event.getSessionId());if(member!=null)publish(member.room());}
}
