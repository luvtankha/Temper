package dev.temper.cerebro;

import dev.temper.cerebro.ai.SentimentModel;
import dev.temper.cerebro.conversation.service.ConversationService;
import dev.temper.cerebro.analysis.service.AnalysisService;
import dev.temper.cerebro.analysis.domain.AnalysisMode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="TEMPER_SENTIMENT_MODEL_DIR",matches=".+")
@SpringBootTest(properties={"temper.toxicity.model-dir=","temper.emotion.model-dir=","temper.sarcasm.model-dir="}) @AutoConfigureMockMvc
class SentimentIntegrationTest {
    @Autowired SentimentModel model;
    @Autowired ConversationService conversations;
    @Autowired AnalysisService analysis;
    @Autowired MockMvc mvc;
    UUID room() {return conversations.create("Genuine sentiment test", List.of("alex", "nova")).id();}
    @Test void actualThreeClassNativeInferenceAndNormalization() {
        var positive=model.classify("I love this wonderful day!");
        var negative=model.classify("I hate this awful day!");
        var neutral=model.classify("The meeting is at three o'clock.");
        assertTrue(positive.probabilities().get("positive")>.9);
        assertTrue(negative.probabilities().get("negative")>.9);
        assertTrue(neutral.probabilities().get("neutral")>.9);
        assertEquals(1, neutral.probabilities().values().stream().mapToDouble(Double::doubleValue).sum(),1e-6);
        assertEquals(neutral.probabilities(),model.classify("The meeting is at three o'clock.").probabilities());
    }
    @Test void sameOrdinalApiDependsOnTextAndPreservesHybridProvenance() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(jsonPath("$.analysisMode").value("HYBRID"));
        var positive=conversations.send(room(),"alex","I love this wonderful day!");
        var negative=conversations.send(room(),"alex","I hate this awful day!");
        var p=analysis.analyze(positive.id());var n=analysis.analyze(negative.id());
        assertEquals(AnalysisMode.HYBRID,p.mode());assertTrue(p.sentiment()>0);assertTrue(n.sentiment()<0);
        assertEquals(p.emotions(),n.emotions());assertNotEquals(p.signals().get("negativeSentiment"),n.signals().get("negativeSentiment"));
        mvc.perform(post("/api/v1/messages/"+positive.id()+"/analyze")).andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("HYBRID"))
            .andExpect(jsonPath("$.evidence[0].source").value("MODEL")).andExpect(jsonPath("$.evidence[1].source").value("MOCK"));
        mvc.perform(get("/api/v1/conversations/"+positive.conversationId()+"/analytics")).andExpect(jsonPath("$.mode").value("HYBRID"));
    }
    @Test void historyActuallyInfluencesFineAndFutureIsExcluded() {
        UUID a=room(),b=room();
        conversations.send(a,"alex","You did an amazing job. I appreciate your help.");
        conversations.send(b,"alex","You always fail. This is terrible and unacceptable.");
        var first=conversations.send(a,"nova","Fine.");var second=conversations.send(b,"nova","Fine.");
        var before=analysis.analyze(first.id());var other=analysis.analyze(second.id());
        assertTrue(Math.abs(before.sentiment()-other.sentiment())>1e-6);
        conversations.send(a,"alex","Future message must not influence the earlier turn.");
        assertEquals(before.sentiment(),analysis.analyze(first.id()).sentiment(),1e-9);
        assertEquals(1,before.contextMessageIds().size());
    }
}
