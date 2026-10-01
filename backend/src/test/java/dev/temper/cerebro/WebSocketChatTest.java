package dev.temper.cerebro;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import dev.temper.cerebro.conversation.service.ConversationService;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"temper.sentiment.model-dir=","temper.foundation.model-dir=","temper.emotion.model-dir="})
class WebSocketChatTest {
    @LocalServerPort int port;@Autowired ConversationService conversations;@Autowired ObjectMapper json;
    StompSession connect(WebSocketStompClient client,UUID room,String speaker) throws Exception {
        var headers=new StompHeaders();headers.add("conversationId",room.toString());headers.add("speakerId",speaker);
        return client.connectAsync("ws://127.0.0.1:"+port+"/ws",new org.springframework.web.socket.WebSocketHttpHeaders(),headers,new StompSessionHandlerAdapter(){@Override public void handleException(StompSession s,StompCommand c,StompHeaders h,byte[] p,Throwable e){throw new AssertionError("STOMP test conversion failed",e);}}).get(10,TimeUnit.SECONDS);
    }
    BlockingQueue<String> subscribe(StompSession session,String topic) {
        var queue=new LinkedBlockingQueue<String>();session.subscribe(topic,new StompFrameHandler(){public Type getPayloadType(StompHeaders headers){return Map.class;}public void handleFrame(StompHeaders headers,Object payload){try{queue.add(json.writeValueAsString(payload));}catch(Exception error){throw new AssertionError(error);}}});return queue;
    }
    @Test void twoNativeClientsDeliverWithoutInferenceAndRejectedSendDoesNotPersist() throws Exception {
        var room=conversations.create("Socket integration",List.of("alex","nova")).id();
        var client=new WebSocketStompClient(new StandardWebSocketClient());var converter=new MappingJackson2MessageConverter();converter.setObjectMapper(json);client.setMessageConverter(converter);
        StompSession a=null,b=null;
        try {
            a=connect(client,room,"alex");b=connect(client,room,"nova");
            String prefix="/topic/conversations/"+room;var messagesA=subscribe(a,prefix+"/messages");var messagesB=subscribe(b,prefix+"/messages");var errors=subscribe(a,"/user/queue/errors");var presence=subscribe(b,prefix+"/presence");
            // Presence response fences earlier SUBSCRIBE frames on the inbound channel.
            b.send("/app/conversations/"+room+"/presence",Map.of());assertNotNull(presence.poll(5,TimeUnit.SECONDS));
            a.send("/app/conversations/"+room+"/presence",Map.of());assertNotNull(presence.poll(5,TimeUnit.SECONDS));
            var requestId=UUID.randomUUID();a.send("/app/conversations/"+room+"/send",Map.of("requestId",requestId,"text","Fictional socket turn"));
            String received=messagesB.poll(5,TimeUnit.SECONDS);assertNotNull(received);var payload=json.readTree(received);assertEquals("alex",payload.at("/message/speakerId").asText());assertEquals(requestId.toString(),payload.get("requestId").asText());assertEquals(1,payload.at("/message/sequence").asInt());assertNotNull(messagesA.poll(5,TimeUnit.SECONDS));assertEquals(1,conversations.messages(room).size());
            a.send("/app/conversations/"+room+"/send",Map.of("requestId",UUID.randomUUID(),"text"," "));assertNotNull(errors.poll(5,TimeUnit.SECONDS));assertEquals(1,conversations.messages(room).size());
        } finally {if(a!=null&&a.isConnected())a.disconnect();if(b!=null&&b.isConnected())b.disconnect();client.stop();}
    }
}
