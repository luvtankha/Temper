package dev.temper.cerebro;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import dev.temper.cerebro.conversation.port.ConversationRepository;
import dev.temper.cerebro.auth.port.UserRepository;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"temper.demo.enabled=false","temper.sentiment.model-dir=","temper.foundation.model-dir="})
class DemoDisabledTest {
    @Autowired ConversationRepository conversations;
    @Autowired UserRepository users;
    @Test void fictionalSeedCanBeDisabledWithoutRequiringDatabaseOrModels() {assertTrue(conversations.findConversations().isEmpty()); assertTrue(users.findUsers().isEmpty());}
}

