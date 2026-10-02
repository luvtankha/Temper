package dev.temper.android;

import android.content.Context;
import android.os.Debug;
import android.os.SystemClock;
import dev.temper.android.adapters.*;
import dev.temper.android.inference.OnDeviceAnalysis;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.File;
import org.json.*;

/** Timing only generated multi-turn exchanges; never obtains an Accessibility root. */
final class PerformanceChecks {
    private interface Analyzer{dev.temper.android.api.LocalAnalysisClient.Result analyze(VisibleConversation snapshot)throws Exception;}
    static void run(Context context,boolean baseline)throws Exception{
        String[] text={"The client moved the deadline again and I only found out from the calendar.",
            "I thought the notification reached you. I can take the first draft tonight.",
            "That helps with tonight, but this is the third time I have had to explain the delay to them.",
            "You're right about the pattern. I'll own the update and put the handoff in writing.",
            "I still need a little time, but that would make it easier to trust the plan."};
        List<VisibleConversation.Turn> turns=new ArrayList<>();
        for(int i=0;i<text.length;i++)turns.add(new VisibleConversation.Turn(String.format(Locale.ROOT,"%064x",i+1),i%2==0?VisibleConversation.Role.REMOTE:VisibleConversation.Role.LOCAL,text[i]));
        var snapshot=new VisibleConversation(VisibleConversation.Status.AVAILABLE,"a".repeat(64),turns,new ScreenObservation.Bounds(0,100,400,150));
        JSONArray times=new JSONArray();
        try(OnDeviceAnalysis current=new OnDeviceAnalysis();BaselineAnalysis46 old=new BaselineAnalysis46()){
            Analyzer engine=snapshotValue->baseline?old.analyze(context,snapshotValue,()->true):current.analyze(context,snapshotValue,()->true);
            for(int i=0;i<8;i++){long started=SystemClock.elapsedRealtimeNanos();var result=engine.analyze(snapshot);times.put((SystemClock.elapsedRealtimeNanos()-started)/1_000_000.0);if(!result.summary().available())throw new AssertionError("Timing fixture had no estimate");}
            Debug.MemoryInfo memory=new Debug.MemoryInfo();Debug.getMemoryInfo(memory);
            JSONObject report=new JSONObject().put("version",BuildConfig.VERSION_NAME).put("generated",true).put("coldMs",times.getDouble(0)).put("allMs",times).put("processPssKb",memory.getTotalPss());
            JSONArray fresh=new JSONArray();
            String[] updates={"I can work with the written plan, but I still need an answer about the earlier delay.","The correction helps, although the client is still expecting us to explain what happened.","I appreciate the offer. Let's confirm who sends the update before making another promise.","That is a start, but we have not settled the handoff for the following week.","I'm willing to try it; please include the revised deadline in the confirmation.","The timing is clearer now, even if I need a little time to trust it again.","Thanks for taking the update. I can concentrate on the draft if that is covered.","We can move forward with that arrangement and review it after the client replies."};
            for(int i=0;i<updates.length;i++){var changed=new ArrayList<>(turns);changed.set(4,new VisibleConversation.Turn(String.format(Locale.ROOT,"%064x",i+20),VisibleConversation.Role.REMOTE,updates[i]));long started=SystemClock.elapsedRealtimeNanos();if(!engine.analyze(new VisibleConversation(VisibleConversation.Status.AVAILABLE,snapshot.conversationKey(),changed,snapshot.composer())).summary().available())throw new AssertionError("Incremental estimate unavailable");fresh.put((SystemClock.elapsedRealtimeNanos()-started)/1_000_000.0);}
            report.put("freshIncomingMs",fresh).put("engine",baseline?"phase45-frozen":"phase46");
            System.gc();System.runFinalization();SystemClock.sleep(200);System.gc();SystemClock.sleep(200);
            Debug.MemoryInfo settled=new Debug.MemoryInfo();Debug.getMemoryInfo(settled);
            report.put("settledProcessPssKb",settled.getTotalPss()).put("javaUsedBytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()).put("nativeAllocatedBytes",Debug.getNativeHeapAllocatedSize());
            Files.write(new File(context.getFilesDir(),"complex-performance.json").toPath(),report.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }
}
