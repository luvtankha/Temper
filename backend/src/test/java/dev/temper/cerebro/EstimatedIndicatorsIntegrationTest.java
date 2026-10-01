package dev.temper.cerebro;
import dev.temper.cerebro.analysis.service.AnalysisService;
import dev.temper.cerebro.conversation.service.ConversationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"temper.indicators.enabled=true","temper.toxicity.model-dir=","temper.sarcasm.model-dir=","temper.emotion.model-dir=","temper.sentiment.model-dir=","temper.foundation.model-dir="})
class EstimatedIndicatorsIntegrationTest {
    @Autowired AnalysisService analysis;@Autowired ConversationService conversations;
    @Test void separatelyStoredIndicatorsUseCausalSpeakerHistory(){
        var room=conversations.create("Estimated indicators",List.of("alex","nova"));
        var a=conversations.send(room.id(),"alex","I disagree.");analysis.analyze(a.id());
        conversations.send(room.id(),"nova","Could you explain?");
        var b=conversations.send(room.id(),"alex","You're wrong. Your fault. Leave me alone.");var result=analysis.analyze(b.id());
        for(String key:List.of("disagreement","repeatedDisagreement","blame","withdrawal"))assertTrue(result.signals().get(key)>0);
        assertEquals(result.signals(),analysis.get(b.id()).signals());assertEquals(result.signals(),analysis.snapshot(room.id()).messages().getLast().signals());
        conversations.send(room.id(),"alex","Future disagreement should not alter old estimates.");assertEquals(result.signals(),analysis.analyze(b.id()).signals());
        assertEquals("UNCERTAIN",result.trajectory().state().name());assertEquals(result.trajectory(),analysis.get(b.id()).trajectory());
        assertNotNull(result.conflictAnalysis());assertTrue(result.conflictAnalysis().rawScore()>=0);assertEquals(result.conflictAnalysis(),analysis.get(b.id()).conflictAnalysis());
        assertEquals("HYBRID",result.mode().name());assertTrue(result.evidence().stream().anyMatch(e->e.label().equals("Estimated repeatedDisagreement")));
    }
}
