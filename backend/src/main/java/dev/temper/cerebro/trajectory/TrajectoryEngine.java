package dev.temper.cerebro.trajectory;
import dev.temper.cerebro.conflict.ConflictEngine;
import java.util.*;
import org.springframework.stereotype.Service;

/** Transparent direction heuristics, not calibrated forecasts. */
@Service
public final class TrajectoryEngine {
    public enum State { STABLE,TENSION_RISING,ESCALATION_RISK,DEESCALATING,WITHDRAWAL_RISK,REPAIR_OPPORTUNITY,UNCERTAIN }
    public record Input(long sequence,ConflictEngine.Result conflict,double sentiment,Map<String,Double> signals){
        public Input{Objects.requireNonNull(conflict);signals=Map.copyOf(signals);if(sequence<1||!Double.isFinite(sentiment)||sentiment < -1||sentiment >1||signals.values().stream().anyMatch(v->v==null||!Double.isFinite(v)||v<0||v>1))throw new IllegalArgumentException("Invalid trajectory input");}
    }
    public record Result(State state,double evidenceStrength,String explanation,Map<String,Double> contributingSignals,List<Long> sequences){}
    private Result uncertain(String reason,List<Long> sequences){return new Result(State.UNCERTAIN,0,reason,Map.of(),sequences);}
    public Result estimate(List<Input> supplied){
        var window=supplied.stream().sorted(Comparator.comparingLong(Input::sequence)).toList();
        if(window.size()>3)window=window.subList(window.size()-3,window.size());
        var sequences=window.stream().map(Input::sequence).toList();
        if(window.size()<3)return uncertain("Current direction is uncertain: at least three analyzed visible turns are needed.",sequences);
        for(int i=1;i<window.size();i++)if(window.get(i).sequence()!=window.get(i-1).sequence()+1)return uncertain("Current direction is uncertain: the recent analyzed window has a gap.",sequences);
        if(window.stream().anyMatch(i->i.conflict().fixtureInputs()))return uncertain("Current direction is uncertain: required analysis channels contain fixtures.",sequences);
        var current=window.getLast();var previous=window.get(window.size()-2);double score=current.conflict().smoothedScore(),prior=previous.conflict().smoothedScore();
        double withdrawal=current.signals().getOrDefault("withdrawal",0d),repeated=current.signals().getOrDefault("repeatedDisagreement",0d);
        State state;double strength;String explanation;
        if(withdrawal>=.55&&prior>=.30){state=State.WITHDRAWAL_RISK;strength=.65;explanation="Recent language may indicate withdrawal after tension.";}
        else if(score>=.55||(current.conflict().trend()==ConflictEngine.Trend.UP&&score>=.40)||repeated>=.70&&score>=.35){state=State.ESCALATION_RISK;strength=.70;explanation="Recent tension signals suggest possible escalation risk.";}
        else if(current.conflict().trend()==ConflictEngine.Trend.DOWN&&score<=.30&&prior>=.35&&current.sentiment()>.15){state=State.REPAIR_OPPORTUNITY;strength=.60;explanation="The conversation appears to be softening; there may be an opportunity for repair.";}
        else if(current.conflict().trend()==ConflictEngine.Trend.DOWN){state=State.DEESCALATING;strength=.60;explanation="The conversation appears to be de-escalating.";}
        else if(current.conflict().trend()==ConflictEngine.Trend.UP){state=State.TENSION_RISING;strength=.60;explanation="Recent tension signals appear to be rising.";}
        else{state=State.STABLE;strength=.55;explanation="Recent tension signals appear relatively stable.";}
        return new Result(state,strength,explanation,Map.of("smoothedConflict",score,"previousSmoothedConflict",prior,"withdrawal",withdrawal,"repeatedDisagreement",repeated),sequences);
    }
}
