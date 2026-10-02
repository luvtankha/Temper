package dev.temper.android.accessibility;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import dev.temper.android.adapters.*;
import dev.temper.android.api.LocalAnalysisClient;
import dev.temper.android.privacy.AnalysisConsent;
import dev.temper.android.analytics.OverlaySummary;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Latest visible window only; bounded pending work, no history or raw diagnostic logging. */
public final class LivePipeline {
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1),new ThreadPoolExecutor.DiscardOldestPolicy());
    private volatile long revision;
    private String fingerprint;
    private final dev.temper.android.inference.OnDeviceAnalysis onDevice=new dev.temper.android.inference.OnDeviceAnalysis();
    public LivePipeline(Context context){this.context=context.getApplicationContext();}
    public void submit(VisibleConversation snapshot,Consumer<LocalAnalysisClient.Result> result,Consumer<OverlaySummary> unavailable){
        cancelIdleRelease();if(worker.isShutdown())return;
        String next=fingerprint(snapshot,AnalysisConsent.local(context));if(next.equals(fingerprint))return;
        fingerprint=next;long expected=++revision,generation=LiveCaptureState.generation();discardPending();unavailable.accept(OverlaySummary.analyzing());LiveCaptureState.status(generation,AnalysisConsent.local(context)?"Analyzing privately on this phone":"Analyzing visible turns on this computer");
        worker.execute(()->{
            LocalAnalysisClient.Result analyzed=null;
            try{if(current(expected,generation)&&AnalysisConsent.allowed(context))analyzed=AnalysisConsent.local(context)?onDevice.analyze(context,snapshot,()->current(expected,generation)&&AnalysisConsent.local(context)):new LocalAnalysisClient().analyze(context,snapshot,()->current(expected,generation));}catch(Exception|LinkageError ignored){}
            LocalAnalysisClient.Result completed=analyzed;
            main.post(()->{if(!current(expected,generation))return;
                if(completed!=null){if(LiveCaptureState.selected()&&AnalysisConsent.local(context)&&completed.summary().available()&&new dev.temper.android.learning.LearningConsent(context).accepted())dev.temper.android.learning.LearningConsent.SESSION.observe(snapshot,completed.summary().spectrum(),completed.summary().currentState(),completed.summary().direction(),completed.trajectory());LiveCaptureState.status(generation,completed.summary().available()?"Model analysis updated":"Analysis unavailable: more supported context or models needed");result.accept(completed);}else{fingerprint=null;LiveCaptureState.status(generation,"Analysis unavailable; check model setup");unavailable.accept(AnalysisConsent.local(context)?OverlaySummary.unavailable():OverlaySummary.connectionUnavailable());}
            });
        });
    }
    public static String fingerprint(VisibleConversation snapshot,boolean local){
        int last=snapshot.turns().size()-1;
        if(local)while(last>=0&&snapshot.turns().get(last).role()!=VisibleConversation.Role.REMOTE)last--;
        StringBuilder key=new StringBuilder(snapshot.conversationKey());for(int i=0;i<=last;i++)key.append(snapshot.turns().get(i).key());return key.toString();
    }
    private boolean idleReleaseScheduled;
    private volatile boolean releaseRequested;
    private void discardPending(){worker.getQueue().removeIf(task->task!=closeModel);}
    private final Runnable closeModel=()->{if(releaseRequested)onDevice.close();};
    private void cancelIdleRelease(){main.removeCallbacks(releaseIdle);idleReleaseScheduled=false;releaseRequested=false;}
    private final Runnable releaseIdle=()->{idleReleaseScheduled=false;releaseRequested=true;if(!worker.isShutdown())worker.execute(closeModel);};
    /** Warm only public model assets when ON is ready, before any host text is read. */
    public void warm(){
        cancelIdleRelease();if(worker.isShutdown())return;
        worker.execute(()->{try{if(new dev.temper.android.privacy.PowerStore(context).enabled()&&AnalysisConsent.local(context))onDevice.prepare(context);}catch(Exception|LinkageError ignored){}finally{if(!new dev.temper.android.privacy.PowerStore(context).enabled()||!AnalysisConsent.local(context))onDevice.close();}});
    }
    /** Release conversation-derived scores immediately; retain public weights for a short return. */
    public void clearContext(){suspend();onDevice.forgetConversation();if(!idleReleaseScheduled&&!worker.isShutdown()){idleReleaseScheduled=true;main.postDelayed(releaseIdle,60_000);}}
    private boolean current(long expected,long generation){
        return expected==revision&&LiveCaptureState.current(generation)&&AnalysisConsent.allowed(context)&&
                (!LiveCaptureState.automatic()||AutomaticCapturePolicy.ready(new dev.temper.android.privacy.PowerStore(context).enabled(),AnalysisConsent.auto(context),dev.temper.android.inference.ModelFiles.ready(context),dev.temper.android.overlay.FloatingOverlayService.running()));
    }
    /** Suppress obsolete estimates and pending snapshots while retaining the loaded local model. */
    public void suspend(){revision++;fingerprint=null;discardPending();}
    public void clear(){cancelIdleRelease();onDevice.forgetConversation();suspend();releaseRequested=true;if(!worker.isShutdown())worker.execute(closeModel);}
    public void close(){if(worker.isShutdown())return;clear();worker.shutdown();}
}
