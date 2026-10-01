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
        String next=snapshot.conversationKey()+snapshot.turns().stream().map(VisibleConversation.Turn::key).reduce("",String::concat);if(next.equals(fingerprint))return;
        fingerprint=next;long expected=++revision,generation=LiveCaptureState.generation();worker.getQueue().clear();unavailable.accept(OverlaySummary.analyzing());LiveCaptureState.status(generation,AnalysisConsent.local(context)?"Analyzing privately on this phone":"Analyzing visible turns on this computer");
        worker.execute(()->{
            LocalAnalysisClient.Result analyzed=null;
            try{if(current(expected,generation)&&AnalysisConsent.allowed(context))analyzed=AnalysisConsent.local(context)?onDevice.analyze(context,snapshot,()->current(expected,generation)&&AnalysisConsent.local(context)):new LocalAnalysisClient().analyze(context,snapshot,()->current(expected,generation));}catch(Exception|LinkageError ignored){}
            LocalAnalysisClient.Result completed=analyzed;
            main.post(()->{if(expected!=revision||!LiveCaptureState.current(generation)||!AnalysisConsent.allowed(context))return;
                if(completed!=null){if(AnalysisConsent.local(context)&&completed.summary().available()&&new dev.temper.android.learning.LearningConsent(context).accepted())dev.temper.android.learning.LearningConsent.SESSION.observe(snapshot,completed.summary().spectrum(),completed.summary().currentState(),completed.summary().direction(),completed.trajectory());LiveCaptureState.status(generation,completed.summary().available()?"Model analysis updated":"Analysis unavailable: more supported context or models needed");result.accept(completed);}else{fingerprint=null;LiveCaptureState.status(generation,"Analysis unavailable; check model setup");unavailable.accept(AnalysisConsent.local(context)?OverlaySummary.unavailable():OverlaySummary.connectionUnavailable());}
            });
        });
    }
    private boolean current(long expected,long generation){return expected==revision&&LiveCaptureState.current(generation);}
    /** Suppress obsolete estimates and pending snapshots while retaining the loaded local model. */
    public void suspend(){revision++;fingerprint=null;worker.getQueue().clear();}
    public void clear(){suspend();if(!worker.isShutdown())worker.execute(onDevice::close);}
    public void close(){clear();worker.getQueue().clear();worker.execute(onDevice::close);worker.shutdown();}
}
