package dev.temper.cerebro;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.temper.cerebro.ai.EmotionModel;
import dev.temper.cerebro.overlay.LiveOverlayService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/** Actual model regressions on generated conversations, not claims about human accuracy. */
@EnabledIfEnvironmentVariables({
    @EnabledIfEnvironmentVariable(named="TEMPER_EMOTION_MODEL_DIR",matches=".+"),
    @EnabledIfEnvironmentVariable(named="TEMPER_SENTIMENT_MODEL_DIR",matches=".+"),
    @EnabledIfEnvironmentVariable(named="TEMPER_SARCASM_MODEL_DIR",matches=".+"),
    @EnabledIfEnvironmentVariable(named="TEMPER_TOXICITY_MODEL_DIR",matches=".+")
})
@SpringBootTest
class LiveSpectrumIntegrationTest {
    record Style(String id,String expected,List<LiveOverlayService.VisibleTurn> turns){}
    @Autowired LiveOverlayService live;
    @Autowired EmotionModel emotion;
    @Autowired ObjectMapper json;
    private List<Style> styles()throws Exception{
        try(var input=getClass().getResourceAsStream("/overlay-chat-styles.json")){
            return Arrays.asList(json.readValue(input,Style[].class));
        }
    }
    private Style style(String id)throws Exception{return styles().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();}
    @Test void localToneCannotChangeRemoteSpectrum()throws Exception{
        var control=live.analyze(new LiveOverlayService.Request(style("role-control-neutral").turns()));
        assertTrue(control.available());
        var direct=EmotionModel.map(emotion.classify("The meeting starts at ten.").probabilities());
        var order=List.of("neutral","happiness","concern","confusion","sadness","frustration","anger","surprise");
        for(int i=0;i<8;i++)assertEquals(direct.get(order.get(i)),control.spectrum().get(i),1e-12);
        for(String id:List.of("role-control-local-angry","role-control-local-happy")){
            var result=live.analyze(new LiveOverlayService.Request(style(id).turns()));
            assertTrue(result.available());assertEquals(control.spectrum(),result.spectrum(),"Other speaker leaked into "+id);
        }
    }
    @Test void everyDummyStyleProducesTheSameEightBoundedRemoteModelValues()throws Exception{
        var order=List.of("neutral","happiness","concern","confusion","sadness","frustration","anger","surprise");
        for(var style:styles()){
            var result=live.analyze(new LiveOverlayService.Request(style.turns()));
            assertTrue(result.available(),style.id());assertEquals(8,result.spectrum().size());
            var remote=style.turns().stream().filter(t->t.role().equals("REMOTE")).reduce((a,b)->b).orElseThrow();
            var direct=EmotionModel.map(emotion.classify(remote.text()).probabilities());
            for(int i=0;i<8;i++)assertEquals(direct.get(order.get(i)),result.spectrum().get(i),1e-12,style.id()+" / "+order.get(i));
            assertTrue(result.spectrum().stream().allMatch(v->Double.isFinite(v)&&v>=0&&v<=1),style.id());
        }
    }
}
