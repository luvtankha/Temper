package dev.temper.android;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import dev.temper.android.accessibility.LiveCaptureState;
import dev.temper.android.accessibility.LivePipeline;
import dev.temper.android.accessibility.TemperAccessibilityService;
import dev.temper.android.adapters.ScreenObservation;
import dev.temper.android.adapters.VisibleConversation;
import dev.temper.android.api.LocalAnalysisClient;
import dev.temper.android.learning.LearningConsent;
import dev.temper.android.privacy.AnalysisConsent;
import dev.temper.android.privacy.ConsentStore;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Generated turns only; uses isolated permission preferences and never obtains a host root. */
final class PipelineContinuityChecks {
    private static VisibleConversation snapshot(String suffix,String remote){
        return new VisibleConversation(VisibleConversation.Status.AVAILABLE,"a".repeat(64),List.of(
                new VisibleConversation.Turn("b".repeat(64),VisibleConversation.Role.LOCAL,"When is the fictional meeting?"),
                new VisibleConversation.Turn(suffix.repeat(64),VisibleConversation.Role.REMOTE,remote),
                new VisibleConversation.Turn("d".repeat(64),VisibleConversation.Role.LOCAL,"Okay, see you then.")),
                new ScreenObservation.Bounds(0,100,400,150));
    }
    static void run(Instrumentation instrumentation,Context target)throws Exception{
        if(TemperAccessibilityService.connected()||LiveCaptureState.active()||LiveCaptureState.armed()||LearningConsent.SESSION.review()!=null)throw new AssertionError("Pipeline fixture requires an idle service and no pending user session");
        Context isolated=new ContextWrapper(target){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("test_pipeline_continuity_44_"+name,mode);}
        };
        ConsentStore consent=new ConsentStore(isolated);
        consent.preferences().edit().clear().commit();
        AtomicReference<LivePipeline> pipeline=new AtomicReference<>();
        AtomicInteger staleResults=new AtomicInteger();
        var fixture=snapshot("c","The meeting starts at ten.");
        try{
            instrumentation.runOnMainSync(()->{
                AnalysisConsent.acceptLocal(isolated);consent.pause(false);
                if(!LiveCaptureState.arm(isolated)||!LiveCaptureState.bind(fixture.conversationKey(),7))throw new AssertionError("Fixture selection failed");
                pipeline.set(new LivePipeline(isolated));
                pipeline.get().submit(fixture,result->staleResults.incrementAndGet(),summary->{});
                pipeline.get().suspend();
            });
            long generation=LiveCaptureState.generation();
            // Leave enough time for an already-queued model task/main callback to complete.
            CountDownLatch quiet=new CountDownLatch(1);quiet.await(1500,TimeUnit.MILLISECONDS);
            instrumentation.waitForIdleSync();
            if(staleResults.get()!=0||!LiveCaptureState.current(generation))throw new AssertionError("Suspension published obsolete estimates or stopped selection");
            CountDownLatch completed=new CountDownLatch(1);
            AtomicReference<LocalAnalysisClient.Result> fresh=new AtomicReference<>();
            instrumentation.runOnMainSync(()->{
                if(!LiveCaptureState.bind(fixture.conversationKey(),8))throw new AssertionError("Keyboard window replacement required rearming");
                pipeline.get().submit(fixture,result->{fresh.set(result);completed.countDown();},summary->{});
            });
            if(!completed.await(45,TimeUnit.SECONDS))throw new AssertionError("Identical visible text did not resume after suspension");
            if(fresh.get()==null||!fresh.get().summary().available()||fresh.get().summary().spectrum()[0]<.7f||!LiveCaptureState.current(generation))throw new AssertionError("Resumed actual model estimate or selected generation incorrect");
            instrumentation.runOnMainSync(()->{
                pipeline.get().submit(snapshot("e","This fictional situation makes me angry."),result->staleResults.incrementAndGet(),summary->{});
                LiveCaptureState.clear();pipeline.get().clear();
            });
            quiet.await(1500,TimeUnit.MILLISECONDS);instrumentation.waitForIdleSync();
            if(staleResults.get()!=0||LiveCaptureState.current(generation))throw new AssertionError("Explicit stop accepted a late estimate");
        }finally{
            instrumentation.runOnMainSync(()->{LiveCaptureState.clear();if(pipeline.get()!=null){pipeline.get().close();pipeline.get().close();}});
            consent.preferences().edit().clear().commit();
            isolated.getSharedPreferences("temper_learning",0).edit().clear().commit();
        }
    }
}
