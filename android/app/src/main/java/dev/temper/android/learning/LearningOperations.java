package dev.temper.android.learning;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** One explicit request at a time, independent of Activity recreation. No retry queue. */
public final class LearningOperations {
    public static final LearningOperations SHARED=new LearningOperations(Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"temper-feedback");thread.setDaemon(true);return thread;}));
    public record State(boolean busy,String message){}
    @FunctionalInterface public interface Action {String run()throws Exception;}
    private final Executor executor;
    private final Set<Runnable> listeners=new CopyOnWriteArraySet<>();
    private boolean busy;
    private String message;
    public LearningOperations(Executor executor){this.executor=executor;}
    public synchronized State state(){return new State(busy,message);}
    public void listen(Runnable listener){listeners.add(listener);}
    public void removeListener(Runnable listener){listeners.remove(listener);}
    public boolean start(Action action){
        synchronized(this){if(busy)return false;busy=true;message=null;}
        changed();
        try{executor.execute(()->{
            String result;
            try{result=action.run();}catch(Exception failure){result="Feedback request was not confirmed. Retry manually.";}
            synchronized(this){message=result;busy=false;}
            changed();
        });}catch(RuntimeException unavailable){synchronized(this){busy=false;message="Feedback request could not start. Retry manually.";}changed();return false;}
        return true;
    }
    private void changed(){for(Runnable listener:listeners)try{listener.run();}catch(RuntimeException ignored){/* A detached screen must not prevent request completion. */}}
}
