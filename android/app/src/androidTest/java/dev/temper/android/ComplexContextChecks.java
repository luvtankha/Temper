package dev.temper.android;

import android.content.Context;
import dev.temper.android.adapters.*;
import dev.temper.android.inference.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.json.*;

/** Real phone inference on fictional held-out chats, never a host Accessibility root. */
final class ComplexContextChecks {
    static void run(Context assets,Context context)throws Exception{
        JSONArray cases;try(var input=assets.getAssets().open("complex-context.json")){cases=new JSONArray(new String(BoundedIo.read(input,400_000),StandardCharsets.UTF_8));}
        JSONArray report=new JSONArray();int checked=0;
        try(OnDeviceAnalysis engine=new OnDeviceAnalysis();BaselineAnalysis46 baseline=new BaselineAnalysis46()){
            for(int i=0;i<cases.length();i++){
                JSONObject fixture=cases.getJSONObject(i);List<VisibleConversation.Turn> turns=new ArrayList<>();JSONArray raw=fixture.getJSONArray("turns");
                for(int j=0;j<raw.length();j++){var turn=raw.getJSONObject(j);turns.add(new VisibleConversation.Turn(String.format(Locale.ROOT,"%064x",j+1),VisibleConversation.Role.valueOf(turn.getString("role")),turn.getString("text")));}
                var snapshot=new VisibleConversation(VisibleConversation.Status.AVAILABLE,WhatsAppAdapter.hash(fixture.getString("id")),turns,new ScreenObservation.Bounds(0,100,400,150));
                var result=engine.analyze(context,snapshot,()->true);
                if(!result.summary().available()||!fixture.getString("candidate").equals(result.trajectory()))throw new AssertionError("Phone context mismatch: "+fixture.getString("id"));
                var old=baseline.analyze(context,snapshot,()->true);for(int j=0;j<8;j++)if(Math.abs(result.summary().spectrum()[j]-old.summary().spectrum()[j])>1e-6)throw new AssertionError("Optimization changed phone spectrum: "+fixture.getString("id"));
                double maximumDrift=0;for(int j=0;j<8;j++)maximumDrift=Math.max(maximumDrift,Math.abs(result.summary().spectrum()[j]-fixture.getJSONArray("current").getDouble(j)));
                // Same conversation: add a repair and an incoming response. No history restart.
                if(fixture.getString("direction").equals("TENSION_RISING")){
                    var continuation=new ArrayList<>(turns);
                    continuation.add(new VisibleConversation.Turn("e".repeat(64),VisibleConversation.Role.LOCAL,"That was my mistake. I'm sorry. I'll correct the plan and confirm it with you first next time."));
                    continuation.add(new VisibleConversation.Turn("f".repeat(64),VisibleConversation.Role.REMOTE,"I'm still upset, but that would help. I appreciate you taking responsibility; let's try the new plan together."));
                    var softened=engine.analyze(context,new VisibleConversation(VisibleConversation.Status.AVAILABLE,snapshot.conversationKey(),continuation,snapshot.composer()),()->true);
                    if(!"DEESCALATING".equals(softened.trajectory()))throw new AssertionError("Direction did not change after repair: "+fixture.getString("id"));
                }
                checked++;report.put(new JSONObject().put("id",fixture.getString("id")).put("language",fixture.getString("language")).put("direction",result.trajectory()).put("maxArmVsDesktopScoreDifference",maximumDrift).put("scores",new JSONArray(result.summary().spectrum())));
            }
            var first=new VisibleConversation(VisibleConversation.Status.AVAILABLE,"f".repeat(64),List.of(new VisibleConversation.Turn("1".repeat(64),VisibleConversation.Role.REMOTE,"I don't understand why the plan changed without a discussion.")),new ScreenObservation.Bounds(0,100,400,150));
            if(!engine.analyze(context,first,()->true).summary().available())throw new AssertionError("First verified incoming turn did not produce a spectrum");
            try{engine.analyze(context,first,()->false);throw new AssertionError("Stopped analysis was accepted");}catch(IllegalStateException expected){}
        }
        if(checked!=28)throw new AssertionError("Missing complex held-out fixtures");
        Files.write(new File(context.getFilesDir(),"complex-context-results.json").toPath(),report.toString(2).getBytes(StandardCharsets.UTF_8));
    }
}
