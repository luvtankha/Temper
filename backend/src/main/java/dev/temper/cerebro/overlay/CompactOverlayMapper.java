package dev.temper.cerebro.overlay;
import dev.temper.cerebro.analysis.service.AnalysisService;
import dev.temper.cerebro.trajectory.TrajectoryEngine;
import java.util.*;
import org.springframework.stereotype.Service;

/** Rendering-independent remote-participant mapping. No raw message text leaves this transform. */
@Service
public final class CompactOverlayMapper {
    public enum CharacterState { NEUTRAL,HAPPY,CONCERNED,CONFUSED,SAD,FRUSTRATED,ANGRY,SURPRISED }
    public record Bar(String emotion,double value){}
    public record View(boolean available,CharacterState character,String currentState,String direction,List<Bar> spectrum,UUID remoteMessageId){}
    private static final List<String> ORDER=List.of("happiness","concern","confusion","sadness","frustration","anger","surprise","neutral");
    public View unavailable(){return new View(false,CharacterState.NEUTRAL,"Current state: Analysis unavailable","Direction: Uncertain",List.of(),null);}
    public View map(List<AnalysisService.Turn> turns,String localSpeakerId){
        if(localSpeakerId==null||localSpeakerId.isBlank()||turns.isEmpty())return unavailable();
        var ordered=turns.stream().sorted(Comparator.comparingLong(AnalysisService.Turn::sequence)).toList();var latest=ordered.getLast();
        if(latest.trajectory()==null||latest.trajectory().state()==TrajectoryEngine.State.UNCERTAIN)return unavailable();
        // This transform is intentionally two-party. Unknown local/remote role fails closed.
        var speakers=ordered.stream().map(AnalysisService.Turn::speakerId).collect(java.util.stream.Collectors.toSet());
        if(speakers.size()!=2||!speakers.contains(localSpeakerId))return unavailable();
        var remote=ordered.stream().filter(t->!t.speakerId().equals(localSpeakerId)).reduce((a,b)->b).orElse(null);
        if(remote==null||latest.sequence()-remote.sequence()>4||!remote.emotions().keySet().containsAll(ORDER)||remote.evidence().stream().noneMatch(e->e.source().name().equals("MODEL")&&e.label().equals("GoEmotions emotion")))return unavailable();
        var bars=ORDER.stream().map(key->new Bar(key,remote.emotions().get(key))).toList();
        if(bars.stream().anyMatch(b->!Double.isFinite(b.value())||b.value()<0||b.value()>1))return unavailable();
        var dominant=bars.stream().max(Comparator.comparingDouble(Bar::value)).orElseThrow();
        CharacterState character=dominant.value()<.25?CharacterState.NEUTRAL:switch(dominant.emotion()){case "happiness"->CharacterState.HAPPY;case "concern"->CharacterState.CONCERNED;case "confusion"->CharacterState.CONFUSED;case "sadness"->CharacterState.SAD;case "frustration"->CharacterState.FRUSTRATED;case "anger"->CharacterState.ANGRY;case "surprise"->CharacterState.SURPRISED;default->CharacterState.NEUTRAL;};
        String direction=switch(latest.trajectory().state()){case STABLE->"Appears stable";case TENSION_RISING->"Tension may be rising";case ESCALATION_RISK->"Possible escalation risk";case DEESCALATING->"Appears to be de-escalating";case WITHDRAWAL_RISK->"Possible withdrawal";case REPAIR_OPPORTUNITY->"Possible repair opportunity";default->"Uncertain";};
        character=switch(latest.trajectory().state()){case TENSION_RISING->CharacterState.CONCERNED;case ESCALATION_RISK->remote.emotions().get("anger")>=remote.emotions().get("frustration")?CharacterState.ANGRY:CharacterState.FRUSTRATED;case WITHDRAWAL_RISK->CharacterState.SAD;case DEESCALATING,REPAIR_OPPORTUNITY->CharacterState.NEUTRAL;default->character;};
        String state=switch(character){case HAPPY->"Positive language";case CONCERNED->"Concerned language";case CONFUSED->"Confused language";case SAD->"Low-energy language";case FRUSTRATED->"Frustrated language";case ANGRY->"Angry language";case SURPRISED->"Surprised language";default->"Calmer language";};
        return new View(true,character,"Current state: "+state,"Direction: "+direction,bars,remote.messageId());
    }
}
