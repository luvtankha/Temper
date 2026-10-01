package dev.temper.android.accessibility;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import dev.temper.android.adapters.*;
import dev.temper.android.api.LocalAnalysisClient;
import dev.temper.android.privacy.LiveConsent;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Latest visible window only; bounded pending work, no history or raw diagnostic logging. */
public final class LivePipeline {
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1),new ThreadPoolExecutor.DiscardOldestPolicy());
    private long revision;
    private String fingerprint;
    public LivePipeline(Context context){this.context=context.getApplicationContext();}
    public void submit(VisibleConversation snapshot,Consumer<LocalAnalysisClient.Result> result,Runnable unavailable){
        String next=snapshot.conversationKey()+snapshot.turns().stream().map(VisibleConversation.Turn::key).reduce("",String::concat);if(next.equals(fingerprint))return;
        fingerprint=next;long expected=++revision,generation=LiveCaptureState.generation();worker.getQueue().clear();unavailable.run();LiveCaptureState.status(generation,"Analyzing visible turns on this computer");
        worker.execute(()->{
            LocalAnalysisClient.Result analyzed=null;
            try{if(LiveCaptureState.current(generation)&&LiveConsent.allowed(context))analyzed=new LocalAnalysisClient().analyze(context,snapshot,()->LiveCaptureState.current(generation));}catch(Exception ignored){}
            LocalAnalysisClient.Result completed=analyzed;
            main.post(()->{if(expected!=revision||!LiveCaptureState.current(generation)||!LiveConsent.allowed(context))return;
                if(completed!=null){LiveCaptureState.status(generation,completed.summary().available()?"Model analysis updated":"Analysis unavailable: more supported context or models needed");result.accept(completed);}else{fingerprint=null;LiveCaptureState.status(generation,"Analysis connection unavailable");unavailable.run();}
            });
        });
    }
    public void clear(){revision++;fingerprint=null;worker.getQueue().clear();}
    public void close(){clear();worker.shutdownNow();}
}
