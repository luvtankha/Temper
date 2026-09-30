package dev.temper.cerebro.common;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import dev.temper.cerebro.auth.domain.User;
import dev.temper.cerebro.auth.port.UserRepository;
import dev.temper.cerebro.conversation.domain.*;
import dev.temper.cerebro.conversation.port.*;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.chat.port.MessageRepository;

@Configuration
public class DemoSeed {
    public static final UUID CONVERSATION_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    @Bean Clock applicationClock() {return Clock.systemUTC();}
    @Bean
    @ConditionalOnProperty(name = "temper.demo.enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner fictionalDemo(UserRepository users, ConversationRepository conversations, ParticipantRepository participants, MessageRepository messages) {
        return args -> {
            if (conversations.findConversation(CONVERSATION_ID).isPresent()) return;
            users.saveUser(new User("alex", "Alex", User.AvatarVariant.MALE));
            users.saveUser(new User("nova", "Nova", User.AvatarVariant.FEMALE));
            Instant start = Instant.parse("2026-10-01T08:30:00Z");
            conversations.saveConversation(new Conversation(CONVERSATION_ID, "The launch plan", start));
            for (String user : List.of("alex", "nova")) participants.saveParticipant(new Participant(stableId("participant-" + user), CONVERSATION_ID, user));
            String[] speakers = {"nova", "alex", "nova", "alex", "nova", "alex", "nova"};
            String[] texts = {
                "Hey! Have you had a chance to look at the launch plan?",
                "I have! The new direction looks really good. ✨",
                "Glad you think so. I was hoping we could get it ready for Friday.",
                "Friday might be a little tight. There are still a few things to work through.",
                "Oh, I thought we were on the same page about the timeline.",
                "We are. I just want to make sure we give it the attention it deserves.",
                "Okay, let’s work out what’s realistic together."
            };
            for (int i = 0; i < texts.length; i++) messages.saveMessage(new Message(stableId("message-" + (i + 1)), CONVERSATION_ID, speakers[i], texts[i], start.plusSeconds(60L * i), i + 1L));
        };
    }
    public static UUID stableId(String name) {return UUID.nameUUIDFromBytes(("temper-fictional-demo-" + name).getBytes(StandardCharsets.UTF_8));}
}
