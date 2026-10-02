package dev.temper.android;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import dev.temper.android.accessibility.LiveCaptureState;
import dev.temper.android.accessibility.LivePipeline;
import dev.temper.android.accessibility.TemperAccessibilityService;
import dev.temper.android.adapters.ScreenObservation;
import dev.temper.android.adapters.VisibleConversation;
import dev.temper.android.api.LocalAnalysisClient;
import dev.temper.android.inference.ModelFiles;
import dev.temper.android.learning.LearningConsent;
import dev.temper.android.learning.LearningOperations;
import dev.temper.android.overlay.FloatingOverlayService;
import dev.temper.android.privacy.AnalysisConsent;
import dev.temper.android.privacy.ConsentStore;
import dev.temper.android.privacy.PowerStore;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * Own-app windows and generated turns only; never obtains a host Accessibility root.
 * ON uses temporary fixture consent/power because instrumentation disconnects Accessibility.
 * OFF uses the real home button and the foreground notification's STOP intent route.
 */
final class PowerOverlayChecks {
    private PowerOverlayChecks(){}

    private static VisibleConversation snapshot(String conversation,String turnPrefix,String remote){
        return new VisibleConversation(VisibleConversation.Status.AVAILABLE,conversation.repeat(64),List.of(
                new VisibleConversation.Turn(turnPrefix+"1".repeat(63),VisibleConversation.Role.LOCAL,"Can we discuss the fictional plan?"),
                new VisibleConversation.Turn(turnPrefix+"2".repeat(63),VisibleConversation.Role.REMOTE,remote),
                new VisibleConversation.Turn(turnPrefix+"3".repeat(63),VisibleConversation.Role.LOCAL,"Okay, we can review the fictional plan.")),
                new ScreenObservation.Bounds(0,100,400,150));
    }

    static void run(Instrumentation instrumentation,Context target)throws Exception{
        if(TemperAccessibilityService.connected()||LiveCaptureState.active()||LiveCaptureState.armed()||LiveCaptureState.automatic()||LearningConsent.SESSION.review()!=null||LearningOperations.SHARED.state().busy()||FloatingOverlayService.running())throw new AssertionError("Power fixture requires idle capture, overlay and feedback");
        if(!Settings.canDrawOverlays(target)||!ModelFiles.ready(target))throw new AssertionError("Power fixture requires the user-granted overlay permission and installed model");
        SharedPreferences preferences=new ConsentStore(target).preferences();
        Map<String,?> original=new HashMap<>(preferences.getAll());
        AtomicReference<LivePipeline> pipeline=new AtomicReference<>();
        AtomicReference<Activity> home=new AtomicReference<>(),dummy=new AtomicReference<>();
        List<Activity> activities=new ArrayList<>();
        AtomicInteger stoppedResults=new AtomicInteger();
        try{
            home.set(launch(instrumentation,target,MainActivity.class));
            activities.add(home.get());
            instrumentation.runOnMainSync(()->{
                AnalysisConsent.acceptAuto(target);
                new PowerStore(target).setEnabled(true);
                FloatingOverlayService.start(home.get());
            });
            dummy.set(launch(instrumentation,target,DebugOverlayActivity.class));
            activities.add(dummy.get());
            awaitMain(instrumentation,FloatingOverlayService::visible,1000,"ON did not show the companion over TEMPER's dummy screen");

            VisibleConversation neutral=snapshot("a","b","The meeting starts at ten.");
            instrumentation.runOnMainSync(()->{
                if(!new PowerStore(target).enabled()||!LiveCaptureState.beginAutomatic(target)||!LiveCaptureState.selectAutomatic(neutral.conversationKey(),7))throw new AssertionError("Automatic neutral fixture did not start");
                pipeline.set(new LivePipeline(target));
            });
            long firstGeneration=LiveCaptureState.generation();
            LocalAnalysisClient.Result first=analyze(instrumentation,pipeline.get(),neutral);
            if(!first.summary().available()||first.summary().spectrum()[0]<.7f)throw new AssertionError("Automatic neutral fixture did not produce real model bars");

            VisibleConversation angry=snapshot("c","d","Stop ignoring me! I am really angry with you.");
            instrumentation.runOnMainSync(()->{
                if(!LiveCaptureState.selectAutomatic(angry.conversationKey(),8)||LiveCaptureState.current(firstGeneration))throw new AssertionError("Automatic chat switch retained the previous generation");
            });
            long secondGeneration=LiveCaptureState.generation();
            LocalAnalysisClient.Result second=analyze(instrumentation,pipeline.get(),angry);
            if(!second.summary().available()||second.summary().spectrum()[6]<.65f||second.summary().spectrum()[6]<=first.summary().spectrum()[6]+.4f||second.summary().spectrum()[0]>=first.summary().spectrum()[0])throw new AssertionError("Automatic fictional chat switch did not refresh emotional bars");
            if(LearningConsent.SESSION.review()!=null||LearningConsent.SESSION.collecting())throw new AssertionError("Automatic analysis created optional feedback collection");

            // Returning to the existing root does not create an Activity, so
            // startActivitySync would wait forever for a creation callback.
            instrumentation.runOnMainSync(()->dummy.get().finish());
            awaitMain(instrumentation,()->home.get().getWindow().getDecorView().hasWindowFocus(),1000,"Existing TEMPER home did not regain focus");
            instrumentation.runOnMainSync(()->{
                View off=find(home.get().getWindow().getDecorView(),"OFF");
                if(!(off instanceof Button)||find(home.get().getWindow().getDecorView(),"Avatar ON")==null)throw new AssertionError("Home did not present the actual OFF button");
                pipeline.get().submit(snapshot("c","e","Thank you! I feel joyful and delighted today."),result->stoppedResults.incrementAndGet(),summary->{});
                off.performClick();
                if(new PowerStore(target).enabled()||!new ConsentStore(target).paused()||LiveCaptureState.current(secondGeneration)||LiveCaptureState.active()||LiveCaptureState.automatic())throw new AssertionError("Home OFF did not synchronously stop automatic capture");
                if(find(home.get().getWindow().getDecorView(),"Avatar OFF")==null)throw new AssertionError("Home did not refresh its OFF state");
            });
            awaitMain(instrumentation,()->!FloatingOverlayService.running()&&!FloatingOverlayService.visible(),1000,"Home OFF left the companion service or character running");
            waitQuiet(instrumentation);
            if(stoppedResults.get()!=0)throw new AssertionError("Home OFF accepted a pending model result");

            instrumentation.runOnMainSync(()->{new PowerStore(target).setEnabled(true);FloatingOverlayService.start(home.get());});
            long stopSequence=new PowerStore(target).stopSequence();
            dummy.set(launch(instrumentation,target,DebugOverlayActivity.class));
            activities.add(dummy.get());
            awaitMain(instrumentation,FloatingOverlayService::visible,1000,"Second ON did not restore the companion");
            instrumentation.runOnMainSync(()->{
                if(!LiveCaptureState.beginAutomatic(target)||!LiveCaptureState.selectAutomatic(angry.conversationKey(),9))throw new AssertionError("Notification-stop fixture did not start");
                pipeline.get().submit(snapshot("c","f","I am worried and nervous about the results."),result->{if(!new PowerStore(target).enabled())stoppedResults.incrementAndGet();},summary->{});
                dummy.get().startService(new Intent(target,FloatingOverlayService.class).setAction("STOP"));
            });
            awaitMain(instrumentation,()->!new PowerStore(target).enabled()&&!FloatingOverlayService.running()&&!FloatingOverlayService.visible()&&!LiveCaptureState.active(),1000,"Notification OFF route did not stop both services' work");
            if(new PowerStore(target).stopSequence()<=stopSequence)throw new AssertionError("Notification OFF did not cancel pending activations");
            waitQuiet(instrumentation);
            if(stoppedResults.get()!=0||LearningConsent.SESSION.review()!=null)throw new AssertionError("Notification OFF retained a late result or collected feedback");
        }finally{
            instrumentation.runOnMainSync(()->{
                FloatingOverlayService.stop(target);LiveCaptureState.clear();
                if(pipeline.get()!=null)pipeline.get().close();
                for(int i=activities.size()-1;i>=0;i--)if(!activities.get(i).isFinishing())activities.get(i).finish();
            });
            awaitMain(instrumentation,()->!FloatingOverlayService.running(),1000,"Could not stop the temporary fixture companion");
            instrumentation.runOnMainSync(()->restore(preferences,original));
        }
    }

    private static Activity launch(Instrumentation instrumentation,Context target,Class<? extends Activity> activity){
        return instrumentation.startActivitySync(new Intent(target,activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private static LocalAnalysisClient.Result analyze(Instrumentation instrumentation,LivePipeline pipeline,VisibleConversation snapshot)throws Exception{
        CountDownLatch completed=new CountDownLatch(1);AtomicReference<LocalAnalysisClient.Result> result=new AtomicReference<>();
        instrumentation.runOnMainSync(()->pipeline.submit(snapshot,value->{result.set(value);completed.countDown();},summary->{}));
        if(!completed.await(30,TimeUnit.SECONDS)||result.get()==null)throw new AssertionError("Automatic generated-text analysis did not finish");
        return result.get();
    }
    private static void awaitMain(Instrumentation instrumentation,BooleanSupplier condition,long timeout,String failure)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+timeout;AtomicBoolean ready=new AtomicBoolean();
        do{
            instrumentation.runOnMainSync(()->ready.set(condition.getAsBoolean()));
            if(ready.get())return;
            new CountDownLatch(1).await(25,TimeUnit.MILLISECONDS);
        }while(SystemClock.elapsedRealtime()<deadline);
        throw new AssertionError(failure);
    }
    private static void waitQuiet(Instrumentation instrumentation)throws Exception{
        new CountDownLatch(1).await(1500,TimeUnit.MILLISECONDS);instrumentation.waitForIdleSync();
    }
    private static View find(View root,String label){
        if(root instanceof TextView text&&label.contentEquals(text.getText()))return root;
        if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),label);if(found!=null)return found;}
        return null;
    }
    @SuppressWarnings("unchecked")
    private static void restore(SharedPreferences preferences,Map<String,?> original){
        SharedPreferences.Editor editor=preferences.edit().clear();
        for(var entry:original.entrySet()){
            Object value=entry.getValue();String key=entry.getKey();
            if(value instanceof Boolean flag)editor.putBoolean(key,flag);
            else if(value instanceof Integer number)editor.putInt(key,number);
            else if(value instanceof Long number)editor.putLong(key,number);
            else if(value instanceof Float number)editor.putFloat(key,number);
            else if(value instanceof String text)editor.putString(key,text);
            else if(value instanceof Set<?> set)editor.putStringSet(key,(Set<String>)set);
            else throw new AssertionError("Unsupported setup preference type");
        }
        if(!editor.commit())throw new AssertionError("Could not restore setup preferences after the temporary power fixture");
    }
}
