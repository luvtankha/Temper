package dev.temper.cerebro;
import dev.temper.cerebro.ai.SarcasmModel;
import dev.temper.cerebro.analysis.service.AnalysisService;
import dev.temper.cerebro.analysis.domain.AnalysisMode;
import dev.temper.cerebro.conversation.service.ConversationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="TEMPER_SARCASM_MODEL_DIR",matches=".+")
@SpringBootTest(properties={"temper.sentiment.model-dir=","temper.emotion.model-dir=","temper.foundation.model-dir="})
class SarcasmIntegrationTest {
    @Autowired SarcasmModel model;@Autowired AnalysisService analysis;@Autowired ConversationService conversations;
    @Test void actualNativeSarcasmMatchesOriginalReferenceAndLiteralContrast() {
        var sarcastic=model.classify("Oh great, another traffic jam. Just what I needed!");
        var literal=model.classify("The train arrives at six o clock.");
        assertEquals(.9760912,sarcastic.probabilities().get("sarcasm"),1e-4);
        assertEquals(.09978381,literal.probabilities().get("sarcasm"),1e-4);
        assertEquals(1,literal.probabilities().values().stream().mapToDouble(Double::doubleValue).sum(),1e-6);
        assertEquals(literal.probabilities(),model.classify("The train arrives at six o clock.").probabilities());
    }
    @Test void existingAnalysisSarcasmIsIndependentOfFixtureSentimentAndKeepsCausalHistory() {
        var a=conversations.create("Independent sarcasm",List.of("alex","nova"));
        var b=conversations.create("Literal contrast",List.of("alex","nova"));
        var first=conversations.send(a.id(),"alex","Oh great, another traffic jam. Just what I needed!");
        var second=conversations.send(b.id(),"alex","The train arrives at six o clock.");
        var sarcastic=analysis.analyze(first.id());var literal=analysis.analyze(second.id());
        assertEquals(sarcastic.sentiment(),literal.sentiment());
        assertTrue(sarcastic.signals().get("sarcasm")>literal.signals().get("sarcasm"));
        assertEquals(AnalysisMode.HYBRID,sarcastic.mode());
        assertTrue(sarcastic.evidence().stream().anyMatch(e->e.label().equals("Sarcasm classifier")&&e.description().contains("independently of sentiment")));
        var later=conversations.send(a.id(),"nova","Fine.");var before=analysis.analyze(later.id());
        conversations.send(a.id(),"alex","Future sarcasm must not leak backward.");
        assertEquals(before.signals().get("sarcasm"),analysis.analyze(later.id()).signals().get("sarcasm"),1e-9);
        assertEquals(List.of(first.id()),before.contextMessageIds());
    }
}
