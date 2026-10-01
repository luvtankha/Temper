package dev.temper.cerebro;
import dev.temper.cerebro.ai.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EmotionMappingTest {
    @Test void independentSigmoidDoesNotForceCompetingLabelsAndRemainsStable() {
        assertEquals(.5,OnnxTextClassifier.sigmoid(0));
        assertTrue(OnnxTextClassifier.sigmoid(3)+OnnxTextClassifier.sigmoid(3)>1.9);
        assertEquals(1,OnnxTextClassifier.sigmoid(1000));assertEquals(0,OnnxTextClassifier.sigmoid(-1000));
        assertThrows(IllegalArgumentException.class,()->OnnxTextClassifier.sigmoid(Double.NaN));
    }
    @Test void proxyMappingDoesNotConfuseAngerAnnoyanceOrInventConcernClass() {
        Map<String,Double> raw=new HashMap<>();for(String label:List.of("anger","annoyance","sadness","joy","confusion","caring","fear","nervousness","surprise","neutral"))raw.put(label,.1);
        raw.put("anger",.8);raw.put("annoyance",.3);raw.put("fear",.6);raw.put("nervousness",.7);
        var mapped=EmotionModel.map(raw);assertEquals(.8,mapped.get("anger"));assertEquals(.3,mapped.get("frustration"));assertEquals(.7,mapped.get("concern"));assertEquals(8,mapped.size());
    }
}
