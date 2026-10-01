package dev.temper.cerebro;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.temper.cerebro.overlay.*;
import dev.temper.cerebro.analysis.domain.*;
import dev.temper.cerebro.analysis.service.*;
import dev.temper.cerebro.conflict.ConflictEngine;
import dev.temper.cerebro.trajectory.TrajectoryEngine;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class LiveOverlayTest {
    private final ConflictEngine conflicts=new ConflictEngine("negativeSentiment=0.25,anger=0.20,toxicity=0.20,sarcasm=0.15,blame=0.10,defensiveness=0.10","0.6,0.3,0.1",.08);
    private final String token="fictional-test-token-at-least-32-characters";
    private LiveOverlayService service(boolean fixtures){
        EmotionAnalysisService engine=(message,context)->{
            assertEquals(Math.min(5,message.sequence()-1),context.previous().size());
            var emotions=new HashMap<String,Double>();for(String key:List.of("neutral","happiness","concern","confusion","sadness","frustration","anger","surprise"))emotions.put(key,key.equals("happiness")&&message.speakerId().equals("REMOTE")?.8:.05);
            var signals=Map.of("negativeSentiment",.05,"toxicity",.05,"sarcasm",.05,"blame",.05,"defensiveness",.05,"passiveAggression",.05);
            var evidence=new ArrayList<MessageAnalysis.Evidence>();if(!fixtures)for(String label:List.of("GoEmotions emotion","RoBERTa sentiment","Toxicity classifier","Sarcasm classifier","Estimated blame","Estimated defensiveness"))evidence.add(new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.MODEL,label,"synthetic test evidence"));
            return new MessageAnalysis(message.id(),message.speakerId(),message.sequence(),AnalysisMode.HYBRID,emotions,signals,.2,0,List.of(),"Synthetic test result",evidence,Instant.EPOCH);
        };
        return new LiveOverlayService(engine,conflicts,new TrajectoryEngine(),new CompactOverlayMapper());
    }
    private LiveOverlayService.Request window(){return new LiveOverlayService.Request(List.of(new LiveOverlayService.VisibleTurn("LOCAL","Fictional first"),new LiveOverlayService.VisibleTurn("REMOTE","Fictional reply"),new LiveOverlayService.VisibleTurn("LOCAL","Fictional latest")));}
    @Test void remotePresentationUsesCausalModelEvidenceAndNoText()throws Exception{
        var result=service(false).analyze(window());assertTrue(result.available());assertEquals("HAPPY",result.character());assertEquals(.8,result.spectrum().get(1));assertFalse(new ObjectMapper().writeValueAsString(result).contains("Fictional"));assertFalse(window().toString().contains("Fictional"));
        assertFalse(service(true).analyze(window()).available());
    }
    @Test void invalidRolesBoundsAndInsufficientContextFailClosed(){
        assertThrows(IllegalArgumentException.class,()->service(false).analyze(new LiveOverlayService.Request(Collections.nCopies(9,new LiveOverlayService.VisibleTurn("LOCAL","x")))));
        assertThrows(IllegalArgumentException.class,()->service(false).analyze(new LiveOverlayService.Request(List.of(new LiveOverlayService.VisibleTurn("UNKNOWN","secret")))));
        assertThrows(IllegalArgumentException.class,()->service(false).analyze(new LiveOverlayService.Request(List.of(new LiveOverlayService.VisibleTurn("LOCAL","x".repeat(1001))))));
        assertFalse(service(false).analyze(new LiveOverlayService.Request(List.of(new LiveOverlayService.VisibleTurn("LOCAL","hello")))).available());
    }
    @Test void transportRequiresTokenBoundsBodyAndDoesNotEchoInvalidText()throws Exception{
        var controller=new LiveOverlayController(service(false),new ObjectMapper(),token);var request=new MockHttpServletRequest();request.setContent(new ObjectMapper().writeValueAsBytes(window()));
        assertEquals(401,assertThrows(ResponseStatusException.class,()->controller.analyze(request)).getStatusCode().value());
        request.addHeader("Authorization","Bearer "+token);var response=controller.analyze(request);assertTrue(response.getBody().available());assertEquals("no-store",response.getHeaders().getCacheControl());
        var invalid=new MockHttpServletRequest();invalid.addHeader("Authorization","Bearer "+token);invalid.setContent("private malformed input".getBytes());var failure=assertThrows(ResponseStatusException.class,()->controller.analyze(invalid));assertEquals(400,failure.getStatusCode().value());assertFalse(failure.toString().contains("private"));
        var huge=new MockHttpServletRequest();huge.addHeader("Authorization","Bearer "+token);huge.setContent(new byte[65_537]);assertEquals(413,assertThrows(ResponseStatusException.class,()->controller.analyze(huge)).getStatusCode().value());
        assertEquals(401,assertThrows(ResponseStatusException.class,()->new LiveOverlayController(service(false),new ObjectMapper(),"").analyze(new MockHttpServletRequest())).getStatusCode().value());
    }
}
