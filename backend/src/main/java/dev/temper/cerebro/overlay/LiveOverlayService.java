package dev.temper.cerebro.overlay;

import dev.temper.cerebro.analysis.domain.MessageAnalysis;
import dev.temper.cerebro.analysis.service.*;
import dev.temper.cerebro.auth.domain.User;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.conflict.ConflictEngine;
import dev.temper.cerebro.conversation.context.ContextWindow;
import dev.temper.cerebro.trajectory.TrajectoryEngine;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

/** Stateless inference: no repositories, message persistence, identifiers or raw-text response. */
@Service
public final class LiveOverlayService {
    public record VisibleTurn(String role,String text){@Override public String toString(){return "VisibleTurn[redacted]";}}
    public record Request(List<VisibleTurn> turns){@Override public String toString(){return "Request[redacted]";}}
    public record Response(boolean available,String character,String currentState,String direction,List<Double> spectrum,String trajectory,double evidenceStrength){}
    private final EmotionAnalysisService engine;
    private final ConflictEngine conflicts;
    private final TrajectoryEngine trajectories;
    private final CompactOverlayMapper mapper;
    public LiveOverlayService(EmotionAnalysisService engine,ConflictEngine conflicts,TrajectoryEngine trajectories,CompactOverlayMapper mapper){this.engine=engine;this.conflicts=conflicts;this.trajectories=trajectories;this.mapper=mapper;}
    public static Response unavailable(){return new Response(false,"NEUTRAL","Analysis unavailable","Uncertain",Collections.nCopies(8,0d),"UNCERTAIN",0);}
    public static void validate(Request request){
        if(request==null||request.turns()==null||request.turns().isEmpty()||request.turns().size()>8)throw new IllegalArgumentException("Invalid visible window");
        for(var turn:request.turns())if(turn==null||!("LOCAL".equals(turn.role())||"REMOTE".equals(turn.role()))||turn.text()==null||turn.text().isBlank()||turn.text().length()>1000)throw new IllegalArgumentException("Invalid visible turn");
    }
    public Response analyze(Request request){
        validate(request);if(request.turns().size()<3||request.turns().stream().map(VisibleTurn::role).distinct().count()!=2)return unavailable();
        UUID conversation=UUID.randomUUID();Instant now=Instant.now();List<ContextWindow.Turn> context=new ArrayList<>();List<MessageAnalysis> history=new ArrayList<>();List<AnalysisService.Turn> results=new ArrayList<>();List<TrajectoryEngine.Input> direction=new ArrayList<>();
        for(var visible:request.turns()){
            long sequence=context.size()+1;UUID id=UUID.randomUUID();String role=visible.role();
            var current=new ContextWindow.Turn(id,new ContextWindow.Speaker(role,role,User.AvatarVariant.MALE),visible.text(),sequence,now);
            var window=new ContextWindow(conversation,current,context.subList(Math.max(0,context.size()-5),context.size()));
            var analysis=engine.analyze(new Message(id,conversation,role,visible.text(),now,sequence),window);
            var conflict=conflicts.calculate(analysis,history);
            // Real input must never become fixture-derived presentation.
            if(conflict.fixtureInputs())return unavailable();
            direction.add(new TrajectoryEngine.Input(sequence,conflict,analysis.sentiment(),analysis.signals()));var trajectory=trajectories.estimate(direction);
            results.add(new AnalysisService.Turn(id,role,sequence,now,analysis.mode(),analysis.emotions(),analysis.signals(),analysis.sentiment(),conflict.smoothedScore(),List.of(),"",analysis.evidence(),conflict,trajectory));
            history.add(analysis);context.add(current);
        }
        var mapped=mapper.map(results,"LOCAL");if(!mapped.available())return unavailable();
        Map<String,Double> values=new HashMap<>();mapped.spectrum().forEach(bar->values.put(bar.emotion(),bar.value()));
        var spectrum=List.of("neutral","happiness","concern","confusion","sadness","frustration","anger","surprise").stream().map(values::get).toList();
        var trajectory=results.getLast().trajectory();
        return new Response(true,mapped.character().name(),mapped.currentState().replaceFirst("^Current state: ",""),mapped.direction().replaceFirst("^Direction: ",""),spectrum,trajectory.state().name(),trajectory.evidenceStrength());
    }
}
