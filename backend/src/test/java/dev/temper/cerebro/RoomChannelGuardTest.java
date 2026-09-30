package dev.temper.cerebro;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import dev.temper.cerebro.websocket.RoomChannelGuard;
import dev.temper.cerebro.persistence.memory.InMemoryDomainStore;
import dev.temper.cerebro.auth.domain.User;
import dev.temper.cerebro.conversation.domain.*;
import static org.junit.jupiter.api.Assertions.*;
class RoomChannelGuardTest {
    @Test void claimedMemberCanAccessOnlyJoinedRoomAndCannotPublishToBrokerTopics() {
        var store=new InMemoryDomainStore();var room=UUID.randomUUID();store.saveUser(new User("alex","Alex",User.AvatarVariant.MALE));store.saveConversation(new Conversation(room,"Room",java.time.Instant.now()));store.saveParticipant(new Participant(UUID.randomUUID(),room,"alex"));var guard=new RoomChannelGuard(store);
        Map<String,Object> attrs=new HashMap<>();var connect=StompHeaderAccessor.create(StompCommand.CONNECT);connect.setSessionAttributes(attrs);connect.setNativeHeader("conversationId",room.toString());connect.setNativeHeader("speakerId","stranger");connect.setLeaveMutable(true);assertThrows(IllegalArgumentException.class,()->guard.preSend(MessageBuilder.createMessage(new byte[0],connect.getMessageHeaders()),null));
        connect.setNativeHeader("speakerId","alex");guard.preSend(MessageBuilder.createMessage(new byte[0],connect.getMessageHeaders()),null);assertEquals(room,attrs.get("room"));
        for(String destination:List.of("/topic/conversations/"+UUID.randomUUID()+"/messages","/queue/private")){var subscribe=StompHeaderAccessor.create(StompCommand.SUBSCRIBE);subscribe.setSessionAttributes(attrs);subscribe.setDestination(destination);subscribe.setLeaveMutable(true);assertThrows(IllegalArgumentException.class,()->guard.preSend(MessageBuilder.createMessage(new byte[0],subscribe.getMessageHeaders()),null));}
        var send=StompHeaderAccessor.create(StompCommand.SEND);send.setSessionAttributes(attrs);send.setDestination("/topic/conversations/"+room+"/messages");send.setLeaveMutable(true);assertThrows(IllegalArgumentException.class,()->guard.preSend(MessageBuilder.createMessage(new byte[0],send.getMessageHeaders()),null));
    }
}
