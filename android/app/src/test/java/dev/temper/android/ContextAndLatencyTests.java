package dev.temper.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import dev.temper.android.accessibility.ProbeDeadline;
import dev.temper.android.accessibility.LivePipeline;
import dev.temper.android.adapters.*;
import dev.temper.android.inference.*;

public class ContextAndLatencyTests {
    @Test public void eventStormCannotPostponeProbeAndCancellationStartsFresh(){
        var deadline=new ProbeDeadline();assertEquals(150,deadline.schedule(1000,150));
        for(int now=1010;now<=1150;now+=10)assertEquals(1150-now,deadline.schedule(now,300));
        deadline.reset();assertEquals(300,deadline.schedule(2000,300));assertEquals(0,deadline.schedule(2010,0));
        deadline.reset();assertEquals(150,deadline.schedule(3000,150));
    }
    private VisibleConversation snapshot(List<VisibleConversation.Turn> turns){return new VisibleConversation(VisibleConversation.Status.AVAILABLE,"a".repeat(64),turns,new ScreenObservation.Bounds(0,100,400,150));}
    @Test public void outgoingTailDoesNotCancelIncomingInferenceButNewIncomingDoes(){
        var incoming=new VisibleConversation.Turn("b".repeat(64),VisibleConversation.Role.REMOTE,"We still need to agree on the revised plan.");
        var reply=new VisibleConversation.Turn("c".repeat(64),VisibleConversation.Role.LOCAL,"I can review the plan after the meeting.");
        var next=new VisibleConversation.Turn("d".repeat(64),VisibleConversation.Role.REMOTE,"Thank you for making time; that would help.");
        var first=snapshot(List.of(incoming));var tail=snapshot(List.of(incoming,reply));
        assertEquals(LivePipeline.fingerprint(first,true),LivePipeline.fingerprint(tail,true));
        assertNotEquals(LivePipeline.fingerprint(first,false),LivePipeline.fingerprint(tail,false));
        assertNotEquals(LivePipeline.fingerprint(tail,true),LivePipeline.fingerprint(snapshot(List.of(incoming,reply,next)),true));
    }
    @Test public void javaContextFeaturesAndPredictionsMatchTrainingIncludingCounterfactualPairs()throws Exception{
        ConversationContextModel model;try(var input=new FileInputStream("src/main/assets/emotion/context-model.json")){model=new ConversationContextModel(input);}
        JSONArray cases;try(var input=getClass().getResourceAsStream("/context-parity.json")){cases=new JSONArray(new String(input.readAllBytes(),StandardCharsets.UTF_8));}
        assertEquals(80,cases.length());int heldOut=0;Map<String,String> paired=new HashMap<>();
        for(int i=0;i<cases.length();i++){
            JSONObject fixture=cases.getJSONObject(i);JSONArray raw=fixture.getJSONArray("turns");List<VisibleConversation.Turn> turns=new ArrayList<>();
            for(int j=0;j<raw.length();j++)turns.add(new VisibleConversation.Turn(String.format(Locale.ROOT,"%064x",j+1),VisibleConversation.Role.valueOf(raw.getJSONObject(j).getString("role")),raw.getJSONObject(j).getString("text")));
            float[] current=new float[8],previous=new float[8];for(int j=0;j<8;j++){current[j]=(float)fixture.getJSONArray("current").getDouble(j);previous[j]=(float)fixture.getJSONArray("previous").getDouble(j);}
            double[] features=model.features(snapshot(turns),current,previous);for(int j=0;j<features.length;j++)assertEquals(fixture.getString("id"),fixture.getJSONArray("features").getDouble(j),features[j],1e-7);
            double[] probabilities=model.probabilities(features);for(int j=0;j<5;j++)assertEquals(fixture.getJSONArray("probabilities").getDouble(j),probabilities[j],1e-7);
            String prediction=model.predict(features);assertEquals(fixture.getString("candidate"),prediction);
            var result=model.summarize(snapshot(turns),current,previous);assertArrayEquals(current,result.summary().spectrum(),0);assertTrue(result.summary().available());
            // A later LOCAL message must not leak into a prediction of the incoming turn.
            turns.add(new VisibleConversation.Turn("f".repeat(64),VisibleConversation.Role.LOCAL,"I am furious. This is unacceptable."));
            assertArrayEquals(features,model.features(snapshot(turns),current,previous),0);
            if(fixture.getString("split").equals("test")){heldOut++;assertEquals(fixture.getString("direction"),prediction);if(fixture.getString("id").startsWith("pair-"))paired.put(fixture.getString("id"),prediction);}
        }
        assertEquals(28,heldOut);assertEquals(8,paired.size());
        assertNotEquals(paired.get("pair-poster-a-v2"),paired.get("pair-poster-b-v2"));
        assertNotEquals(paired.get("pair-keynote-a-v2"),paired.get("pair-keynote-b-v2"));
    }
}
