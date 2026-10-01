package dev.temper.cerebro;
import dev.temper.cerebro.ai.EmotionModel;
import dev.temper.cerebro.analysis.service.AnalysisService;
import dev.temper.cerebro.analysis.domain.AnalysisMode;
import dev.temper.cerebro.conversation.service.ConversationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="TEMPER_EMOTION_MODEL_DIR",matches=".+")
@SpringBootTest
class EmotionIntegrationTest {
    @Autowired EmotionModel model;@Autowired AnalysisService analysis;@Autowired ConversationService conversations;
    @Test void realIndependentClassifierRespondsToTextAndRetainsAll28Labels() {
        var happy=model.classify("I am so happy and joyful today!");var angry=model.classify("I am furious and angry with you!");
        assertEquals(28,happy.probabilities().size());assertTrue(happy.probabilities().get("joy")>angry.probabilities().get("joy"));
        assertTrue(angry.probabilities().get("anger")>happy.probabilities().get("anger"));
        assertTrue(happy.probabilities().values().stream().allMatch(v->Double.isFinite(v)&&v>=0&&v<=1));
        assertEquals(happy.probabilities(),model.classify("I am so happy and joyful today!").probabilities());
    }
    @Test void sameSequenceExistingApiUsesModelEmotionWithProxyEvidence() {
        var a=conversations.create("Emotion happy",List.of("alex","nova"));var b=conversations.create("Emotion angry",List.of("alex","nova"));
        var happy=analysis.analyze(conversations.send(a.id(),"alex","I am so happy and joyful today!").id());
        var angry=analysis.analyze(conversations.send(b.id(),"alex","I am furious and angry with you!").id());
        assertEquals(AnalysisMode.HYBRID,happy.mode());assertTrue(happy.emotions().get("happiness")>angry.emotions().get("happiness"));
        assertTrue(angry.emotions().get("anger")>happy.emotions().get("anger"));assertTrue(happy.emotions().containsKey("surprise"));
        assertTrue(happy.emotions().containsKey("neutral"));assertTrue(happy.evidence().stream().anyMatch(e->e.label().equals("GoEmotions emotion")&&e.description().contains("frustration proxy")));
        assertTrue(happy.evidence().stream().anyMatch(e->e.label().equals("Remaining fixture signals")&&!e.description().startsWith("Emotions,")));
    }
}
