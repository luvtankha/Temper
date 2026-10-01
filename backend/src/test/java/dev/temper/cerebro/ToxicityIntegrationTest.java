package dev.temper.cerebro;
import dev.temper.cerebro.ai.ToxicityModel;
import dev.temper.cerebro.analysis.service.AnalysisService;
import dev.temper.cerebro.analysis.domain.AnalysisMode;
import dev.temper.cerebro.conversation.service.ConversationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="TEMPER_TOXICITY_MODEL_DIR",matches=".+")
@SpringBootTest(properties={"temper.sentiment.model-dir=","temper.emotion.model-dir=","temper.sarcasm.model-dir=","temper.foundation.model-dir="})
class ToxicityIntegrationTest {
    @Autowired ToxicityModel model;@Autowired AnalysisService analysis;@Autowired ConversationService conversations;
    @Test void nativeIndependentSigmoidsMatchOriginalCheckpoint() {
        var polite=model.classify("Thank you for helping me.");
        var insult=model.classify("You are an idiot.");var threat=model.classify("I will hurt you.");
        assertEquals(.0005192989,polite.probabilities().get("toxic"),1e-4);
        assertEquals(.98780757,insult.probabilities().get("toxic"),1e-4);
        assertEquals(.95732325,insult.probabilities().get("insult"),1e-4);
        assertEquals(.66320497,threat.probabilities().get("threat"),1e-4);
        assertEquals(6,insult.probabilities().size());
        assertTrue(insult.probabilities().values().stream().mapToDouble(Double::doubleValue).sum()>1.5);
        assertTrue(insult.probabilities().values().stream().allMatch(v->v>=0&&v<=1));
        assertEquals(polite.probabilities(),model.classify("Thank you for helping me.").probabilities());
    }
    @Test void toxicityAndExplicitHostilityProxyUseTextAndStrictPriorContext() {
        var a=conversations.create("Toxicity contrast",List.of("alex","nova"));
        var b=conversations.create("Polite contrast",List.of("alex","nova"));
        var first=conversations.send(a.id(),"alex","You are an idiot.");
        var second=conversations.send(b.id(),"alex","Thank you for helping me.");
        var hostile=analysis.analyze(first.id());var polite=analysis.analyze(second.id());
        assertEquals(hostile.sentiment(),polite.sentiment());assertEquals(hostile.signals().get("sarcasm"),polite.signals().get("sarcasm"));
        assertTrue(hostile.signals().get("toxicity")>polite.signals().get("toxicity"));
        assertEquals(Math.max(hostile.signals().get("insult"),hostile.signals().get("threat")),hostile.signals().get("hostility"));
        assertEquals(AnalysisMode.HYBRID,hostile.mode());
        assertTrue(hostile.evidence().stream().anyMatch(e->e.source().name().equals("HEURISTIC")&&e.label().equals("Hostility proxy")));
        var later=conversations.send(a.id(),"nova","Fine.");var before=analysis.analyze(later.id());
        conversations.send(a.id(),"alex","Future text must not leak backward.");
        assertEquals(before.signals(),analysis.analyze(later.id()).signals());assertEquals(List.of(first.id()),before.contextMessageIds());
        assertEquals(hostile.signals(),analysis.snapshot(a.id()).messages().get(0).signals());
    }
}
