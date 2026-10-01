package dev.temper.android.learning;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class LearningOperationsTests {
    private static final class ManualExecutor implements Executor {
        final Queue<Runnable> tasks=new ArrayDeque<>();
        public void execute(Runnable action){tasks.add(action);}
        void finish(){tasks.remove().run();}
    }
    @Test public void newScreenCannotOverlapRequestsAndNoRejectedRequestIsQueued(){
        var executor=new ManualExecutor();var operations=new LearningOperations(executor);var firstScreen=new AtomicInteger();var nextScreen=new AtomicInteger();Runnable first=firstScreen::incrementAndGet;operations.listen(first);
        assertTrue(operations.start(()->"Sent"));assertTrue(operations.state().busy());assertFalse(operations.start(()->{fail("Deletion overlapped send");return "Deleted";}));assertEquals(1,executor.tasks.size());
        operations.removeListener(first);operations.listen(nextScreen::incrementAndGet);executor.finish();assertFalse(operations.state().busy());assertEquals("Sent",operations.state().message());assertEquals(1,firstScreen.get());assertEquals(1,nextScreen.get());
        assertTrue(operations.start(()->"Deleted"));executor.finish();assertEquals("Deleted",operations.state().message());assertTrue(executor.tasks.isEmpty());
    }
    @Test public void failedOperationAndDetachedListenerDoNotDisableFutureExplicitRequests(){
        var executor=new ManualExecutor();var operations=new LearningOperations(executor);operations.listen(()->{throw new IllegalStateException("Detached screen");});
        assertTrue(operations.start(()->{throw new java.io.IOException("Never expose private failure data");}));executor.finish();assertFalse(operations.state().busy());assertFalse(operations.state().message().contains("private"));
        assertTrue(operations.start(()->"Completed"));executor.finish();assertEquals("Completed",operations.state().message());
    }
    @Test public void executorFailureClearsBusyStateWithoutKeepingAnAutomaticRetry(){
        var attempts=new AtomicInteger();var operations=new LearningOperations(action->{if(attempts.getAndIncrement()==0)throw new java.util.concurrent.RejectedExecutionException();action.run();});
        assertFalse(operations.start(()->{fail("Rejected request ran");return "";}));assertFalse(operations.state().busy());assertTrue(operations.start(()->"Manual retry completed"));assertEquals("Manual retry completed",operations.state().message());
    }
    @Test public void onlyAnExplicitBooleanConfirmsSubmissionOrDeletion()throws Exception {
        LearningClient.confirmResponse("{\"stored\":true}","stored");LearningClient.confirmResponse("{\"deleted\":true}","deleted");
        for(String response:new String[]{"{\"deleted\":\"true\"}","{\"deleted\":false}","{\"stored\":true}","{}","<html>OK</html>"})try{LearningClient.confirmResponse(response,"deleted");fail("Unconfirmed deletion was accepted");}catch(java.io.IOException expected){}
    }
}
