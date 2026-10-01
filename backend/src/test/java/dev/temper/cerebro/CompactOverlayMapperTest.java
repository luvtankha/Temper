package dev.temper.cerebro;
import dev.temper.cerebro.overlay.CompactOverlayMapper;
import dev.temper.cerebro.analysis.service.AnalysisService;
import dev.temper.cerebro.analysis.domain.*;
import dev.temper.cerebro.trajectory.TrajectoryEngine;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CompactOverlayMapperTest {
    private final CompactOverlayMapper mapper=new CompactOverlayMapper();
    private AnalysisService.Turn turn(String speaker,long seq,String emotion,TrajectoryEngine.State state,boolean model){
        var emotions=new LinkedHashMap<String,Double>();for(String key:List.of("happiness","concern","confusion","sadness","frustration","anger","surprise","neutral"))emotions.put(key,key.equals(emotion)?.8:.05);
        var trajectory=new TrajectoryEngine.Result(state,.6,"Estimated",Map.of(),List.of(1L,2L,3L));
        return new AnalysisService.Turn(UUID.randomUUID(),speaker,seq,Instant.EPOCH,AnalysisMode.HYBRID,emotions,Map.of(),0,0,List.of(),"Estimated",model?List.of(new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.MODEL,"GoEmotions emotion","Verified model")):List.of(),null,trajectory);
    }
    @Test void mapsRemoteRatherThanLatestLocalEmotionAndKeepsExactlyEightBars(){
        var remote=turn("nova",2,"happiness",TrajectoryEngine.State.STABLE,true);var local=turn("alex",3,"anger",TrajectoryEngine.State.STABLE,true);
        var view=mapper.map(List.of(remote,local),"alex");assertTrue(view.available());assertEquals(remote.messageId(),view.remoteMessageId());assertEquals(CompactOverlayMapper.CharacterState.HAPPY,view.character());assertEquals(8,view.spectrum().size());assertEquals(.8,view.spectrum().getFirst().value());
        assertTrue(view.currentState().length()<65&&view.direction().length()<65);assertEquals(view,mapper.map(List.of(local,remote),"alex"));
        assertEquals(CompactOverlayMapper.CharacterState.ANGRY,mapper.map(List.of(remote,local),"nova").character());
    }
    @Test void unsafeRoleFixtureStaleOrUncertainReturnsNoInventedBars(){
        assertFalse(mapper.map(List.of(),"alex").available());
        var remote=turn("nova",2,"happiness",TrajectoryEngine.State.STABLE,false);var local=turn("alex",3,"anger",TrajectoryEngine.State.STABLE,true);
        assertTrue(mapper.map(List.of(remote,local),"alex").spectrum().isEmpty());assertFalse(mapper.map(List.of(remote,local),"unknown").available());
        assertFalse(mapper.map(List.of(turn("nova",1,"happiness",TrajectoryEngine.State.STABLE,true),turn("alex",7,"anger",TrajectoryEngine.State.STABLE,true)),"alex").available());
        assertFalse(mapper.map(List.of(turn("nova",2,"happiness",TrajectoryEngine.State.STABLE,true),turn("alex",3,"anger",TrajectoryEngine.State.UNCERTAIN,true)),"alex").available());
    }
    @Test void trajectoryAltersExpressionWithoutChangingRemoteSpectrum(){
        var remote=turn("nova",2,"frustration",TrajectoryEngine.State.STABLE,true);
        for(var state:List.of(TrajectoryEngine.State.TENSION_RISING,TrajectoryEngine.State.ESCALATION_RISK,TrajectoryEngine.State.WITHDRAWAL_RISK,TrajectoryEngine.State.DEESCALATING,TrajectoryEngine.State.REPAIR_OPPORTUNITY)){
            var view=mapper.map(List.of(remote,turn("alex",3,"happiness",state,true)),"alex");assertTrue(view.available());assertEquals(.8,view.spectrum().stream().filter(b->b.emotion().equals("frustration")).findFirst().orElseThrow().value());
            var expected=switch(state){case TENSION_RISING->CompactOverlayMapper.CharacterState.CONCERNED;case ESCALATION_RISK->CompactOverlayMapper.CharacterState.FRUSTRATED;case WITHDRAWAL_RISK->CompactOverlayMapper.CharacterState.SAD;default->CompactOverlayMapper.CharacterState.NEUTRAL;};assertEquals(expected,view.character());
        }
    }
}
