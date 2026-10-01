package dev.temper.cerebro;
import dev.temper.cerebro.conflict.ConflictEngine;
import dev.temper.cerebro.analysis.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ConflictEngineTest {
    private final String weights="negativeSentiment=.25,anger=.20,toxicity=.20,sarcasm=.15,blame=.10,defensiveness=.10";
    private final ConflictEngine engine=new ConflictEngine(weights,".6,.3,.1",.08);
    private MessageAnalysis row(long seq,double value){
        return new MessageAnalysis(UUID.randomUUID(),"alex",seq,AnalysisMode.MOCK,Map.of("frustration",value,"anger",value,"sadness",0d,"happiness",0d,"confusion",0d,"concern",0d),Map.of("negativeSentiment",value,"sarcasm",value,"toxicity",value,"passiveAggression",0d,"defensiveness",value,"blame",value),0,0,List.of(),"Fixture",List.of(),Instant.EPOCH);
    }
    @Test void exactWeightedSmoothingAndCausalTrend(){
        var a=row(1,0);var b=row(2,1);var c=row(3,.5);var result=engine.calculate(c,List.of(a,b,row(4,1)));
        assertEquals(.5,result.rawScore(),1e-9);assertEquals(.6,result.smoothedScore(),1e-9);
        assertEquals(ConflictEngine.Trend.FLAT,result.trend());
        assertEquals(ConflictEngine.Trend.DOWN,engine.calculate(row(3,.2),List.of(a,b)).trend());
        assertEquals(ConflictEngine.Trend.UP,engine.calculate(b,List.of(a)).trend());assertEquals(List.of(1L,2L),result.priorSequences());
        assertEquals("negativeSentiment",result.topContributors().getFirst().signal());assertTrue(result.fixtureInputs());
        assertEquals(result,engine.calculate(c,List.of(b,a)));
    }
    @Test void earlyAndMissingPredecessorsNormalizeOnlyAvailableWeights(){
        assertEquals(.4,engine.calculate(row(1,.4),List.of()).smoothedScore(),1e-9);
        assertEquals(2d/3,engine.calculate(row(2,1),List.of(row(1,0))).smoothedScore(),1e-9);
        assertEquals(.6/.7,engine.calculate(row(3,1),List.of(row(1,0))).smoothedScore(),1e-9);
        assertEquals(ConflictEngine.Trend.FLAT,engine.calculate(row(3,1),List.of(row(1,0))).trend());
    }
    @Test void configurationRejectsInvalidWeightsAndCustomWeightsChangeContributions(){
        assertThrows(IllegalArgumentException.class,()->new ConflictEngine(weights.replace(".25","-.25"),".6,.3,.1",.08));
        assertThrows(IllegalArgumentException.class,()->new ConflictEngine(weights,"NaN,.3,.1",.08));
        assertThrows(IllegalArgumentException.class,()->new ConflictEngine(weights,".6,.3,.1",0));
        var custom=new ConflictEngine("negativeSentiment=0,anger=1,toxicity=0,sarcasm=0,blame=0,defensiveness=0","1,0,0",.1);
        assertEquals("anger",custom.calculate(row(1,.7),List.of()).topContributors().getFirst().signal());
        assertEquals(.7,custom.calculate(row(1,.7),List.of()).rawScore(),1e-9);
    }
}
