package dev.temper.cerebro;
import dev.temper.cerebro.analysis.service.EstimatedLanguageIndicators;
import dev.temper.cerebro.conversation.context.ContextWindow;
import dev.temper.cerebro.auth.domain.User;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EstimatedLanguageIndicatorsTest {
    private final EstimatedLanguageIndicators engine=new EstimatedLanguageIndicators(true);
    private ContextWindow.Turn turn(String speaker,String text,long sequence){return new ContextWindow.Turn(UUID.randomUUID(),new ContextWindow.Speaker(speaker,speaker,User.AvatarVariant.MALE),text,sequence,Instant.EPOCH);}
    private EstimatedLanguageIndicators.Result estimate(String text){return engine.estimate(new ContextWindow(UUID.randomUUID(),turn("alex",text,1),List.of()));}
    @Test void sixIndependentEstimatesExposeRulesAndRemainBounded(){
        var result=estimate("Whatever you say. Your fault; you always do this. Not my fault. I disagree. Leave me alone.");
        for(String key:List.of("passiveAggression","blame","defensiveness","disagreement","withdrawal"))assertTrue(result.scores().get(key)>0);
        assertEquals(0,result.scores().get("repeatedDisagreement"));assertEquals(6,result.evidence().size());
        assertTrue(result.evidence().stream().allMatch(e->e.source().name().equals("HEURISTIC")&&(e.description().contains("Estimated")||e.description().contains("estimated"))));
        assertTrue(result.scores().values().stream().allMatch(v->v>=0&&v<=.85));
        assertTrue(estimate("Thank you for your help.").scores().values().stream().allMatch(v->v==0));
    }
    @Test void quotedCuesAndSupportedNegationDoNotTrigger(){
        assertEquals(0,estimate("She said \"your fault\"; it is not your fault. I don't disagree.").scores().get("blame"));
        assertEquals(0,estimate("I don't disagree.").scores().get("disagreement"));
        assertEquals(0,estimate("Other people say “leave me alone”.").scores().get("withdrawal"));
        assertTrue(estimate("I’m done talking.").scores().get("withdrawal")>0);
    }
    @Test void repetitionRequiresCurrentAndSameSpeakerPriorDisagreement(){
        var current=turn("alex","I disagree.",3);var room=UUID.randomUUID();
        assertTrue(engine.estimate(new ContextWindow(room,current,List.of(turn("alex","You're wrong.",1),turn("nova","Okay.",2)))).scores().get("repeatedDisagreement")>0);
        assertEquals(0,engine.estimate(new ContextWindow(room,current,List.of(turn("nova","I disagree.",1)))).scores().get("repeatedDisagreement"));
        assertEquals(0,engine.estimate(new ContextWindow(room,turn("alex","Okay.",3),List.of(turn("alex","I disagree.",1)))).scores().get("repeatedDisagreement"));
        assertThrows(IllegalArgumentException.class,()->new ContextWindow(room,current,List.of(turn("alex","I disagree.",4))));
    }
}
