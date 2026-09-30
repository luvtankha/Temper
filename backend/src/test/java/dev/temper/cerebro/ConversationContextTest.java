package dev.temper.cerebro;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import dev.temper.cerebro.persistence.memory.InMemoryDomainStore;
import dev.temper.cerebro.auth.domain.User;
import dev.temper.cerebro.conversation.domain.*;
import dev.temper.cerebro.conversation.context.*;
import dev.temper.cerebro.chat.domain.Message;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class ConversationContextTest {
    final InMemoryDomainStore store=new InMemoryDomainStore();final UUID room=UUID.randomUUID();final List<Message> messages=new ArrayList<>();
    ConversationContextTest(){
        store.saveConversation(new Conversation(room,"Context fixture",Instant.EPOCH));
        for(String id:List.of("alex","nova")){store.saveUser(new User(id,id.equals("alex")?"Alex":"Nova",id.equals("alex")?User.AvatarVariant.MALE:User.AvatarVariant.FEMALE));store.saveParticipant(new Participant(UUID.randomUUID(),room,id));}
        for(int i=1;i<=9;i++){var message=new Message(UUID.randomUUID(),room,i%2==0?"alex":"nova",i==7?"Fine.":"Fictional turn "+i,Instant.EPOCH.plusSeconds(100-i),i);messages.add(message);store.saveMessage(message);}
    }
    RepositoryConversationContextService context(int count){return new RepositoryConversationContextService(store,store,store,count);}
    @Test void boundedCausalWindowUsesSequenceAndSpeakerRatherThanTimestamp() {
        var window=context(5).buildContext(room,messages.get(6).id());assertEquals("Fine.",window.current().text());assertEquals("Nova",window.current().speaker().displayName());assertEquals(List.of(2L,3L,4L,5L,6L),window.previous().stream().map(ContextWindow.Turn::sequence).toList());
        assertFalse(window.previous().stream().anyMatch(t->t.messageId().equals(messages.get(7).id())));assertThrows(UnsupportedOperationException.class,()->window.previous().clear());
        assertEquals(List.of(4L,5L,6L),context(3).buildContext(room,messages.get(6).id()).previous().stream().map(ContextWindow.Turn::sequence).toList());
        assertTrue(context(5).buildContext(room,messages.getFirst().id()).previous().isEmpty());assertEquals(1,context(5).buildContext(room,messages.get(1).id()).previous().size());
    }
    @Test void rejectsForeignOrMissingTurnAndInvalidWindowConfiguration() {
        var foreign=UUID.randomUUID();store.saveConversation(new Conversation(foreign,"Other room",Instant.EPOCH));assertThrows(IllegalArgumentException.class,()->context(5).buildContext(foreign,messages.getFirst().id()));
        assertThrows(ResponseStatusException.class,()->context(5).buildContext(room,UUID.randomUUID()));assertThrows(ResponseStatusException.class,()->context(5).buildContext(UUID.randomUUID(),messages.getFirst().id()));
        assertThrows(IllegalArgumentException.class,()->context(2));assertThrows(IllegalArgumentException.class,()->context(6));
    }
    @Test void immutableWindowRejectsCurrentDuplicateUnorderedAndFutureReferences() {
        var window=context(5).buildContext(room,messages.get(6).id());var first=window.previous().getFirst();
        assertThrows(IllegalArgumentException.class,()->new ContextWindow(room,window.current(),List.of(window.current())));
        assertThrows(IllegalArgumentException.class,()->new ContextWindow(room,window.current(),List.of(first,first)));
        assertThrows(IllegalArgumentException.class,()->new ContextWindow(room,window.current(),List.of(window.previous().getLast(),first)));
    }
}
