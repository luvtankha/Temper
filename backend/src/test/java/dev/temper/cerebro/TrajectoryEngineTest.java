package dev.temper.cerebro;
import dev.temper.cerebro.conflict.ConflictEngine;
import dev.temper.cerebro.trajectory.TrajectoryEngine;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TrajectoryEngineTest {
    private final TrajectoryEngine engine=new TrajectoryEngine();
    private TrajectoryEngine.Input input(long seq,double conflict,ConflictEngine.Trend trend,double sentiment,double withdrawal,boolean fixture){return new TrajectoryEngine.Input(seq,new ConflictEngine.Result(conflict,conflict,trend,List.of(),fixture,List.of()),sentiment,Map.of("withdrawal",withdrawal));}
    private TrajectoryEngine.Result result(double previous,double current,ConflictEngine.Trend trend,double sentiment,double withdrawal){return engine.estimate(List.of(input(1,previous,ConflictEngine.Trend.FLAT,0,0,false),input(2,previous,ConflictEngine.Trend.FLAT,0,0,false),input(3,current,trend,sentiment,withdrawal,false)));}
    @Test void allSixActionableStatesAreDeterministicAndBounded(){
        assertEquals(TrajectoryEngine.State.STABLE,result(.1,.1,ConflictEngine.Trend.FLAT,0,0).state());
        assertEquals(TrajectoryEngine.State.TENSION_RISING,result(.1,.25,ConflictEngine.Trend.UP,0,0).state());
        assertEquals(TrajectoryEngine.State.ESCALATION_RISK,result(.3,.5,ConflictEngine.Trend.UP,0,0).state());
        assertEquals(TrajectoryEngine.State.DEESCALATING,result(.5,.35,ConflictEngine.Trend.DOWN,0,0).state());
        assertEquals(TrajectoryEngine.State.REPAIR_OPPORTUNITY,result(.4,.2,ConflictEngine.Trend.DOWN,.4,0).state());
        assertEquals(TrajectoryEngine.State.WITHDRAWAL_RISK,result(.4,.5,ConflictEngine.Trend.UP,0,.55).state());
        var a=result(.1,.25,ConflictEngine.Trend.UP,0,0);assertEquals(a,result(.1,.25,ConflictEngine.Trend.UP,0,0));assertTrue(a.evidenceStrength()>0&&a.evidenceStrength()<1);
    }
    @Test void insufficientGappedOrFixtureContextFailsClosed(){
        assertEquals(TrajectoryEngine.State.UNCERTAIN,engine.estimate(List.of()).state());
        assertEquals(TrajectoryEngine.State.UNCERTAIN,engine.estimate(List.of(input(1,.1,ConflictEngine.Trend.FLAT,0,0,false),input(2,.2,ConflictEngine.Trend.UP,0,0,false))).state());
        assertEquals(TrajectoryEngine.State.UNCERTAIN,engine.estimate(List.of(input(1,.1,ConflictEngine.Trend.FLAT,0,0,false),input(3,.2,ConflictEngine.Trend.UP,0,0,false),input(4,.3,ConflictEngine.Trend.UP,0,0,false))).state());
        var uncertain=engine.estimate(List.of(input(1,.1,ConflictEngine.Trend.FLAT,0,0,true),input(2,.2,ConflictEngine.Trend.UP,0,0,false),input(3,.3,ConflictEngine.Trend.UP,0,0,false)));
        assertEquals(TrajectoryEngine.State.UNCERTAIN,uncertain.state());assertEquals(0,uncertain.evidenceStrength());
    }
}
