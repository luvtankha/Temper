package dev.temper.android.learning;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LearningDeletionKeyTests {
    @Rule public TemporaryFolder folder=new TemporaryFolder();
    @Test public void concurrentActivityInstancesUseOnePersistedAnonymousKey()throws Exception {
        File file=new File(folder.getRoot(),"deletion-key");assertNull(new LearningDeletionKey(file).readOrCreate(false));var worker=Executors.newFixedThreadPool(4);
        try{var results=new ArrayList<Future<String>>();for(int i=0;i<12;i++)results.add(worker.submit(()->new LearningDeletionKey(file).readOrCreate(true)));String token=results.get(0).get();assertEquals(43,token.length());for(var result:results)assertEquals(token,result.get());assertEquals(token,new String(Files.readAllBytes(file.toPath()),StandardCharsets.US_ASCII));}finally{worker.shutdownNow();}
    }
    @Test public void anOldDeletionConfirmationCannotForgetAReplacementKey()throws Exception {
        File file=new File(folder.getRoot(),"deletion-key");var keys=new LearningDeletionKey(file);String original=keys.readOrCreate(true);keys.forget(original);String replacement=keys.readOrCreate(true);assertNotEquals(original,replacement);
        try{keys.forget(original);fail("Stale deletion removed a replacement key");}catch(java.io.IOException expected){}
        assertEquals(replacement,keys.readOrCreate(false));keys.forget(replacement);assertNull(keys.readOrCreate(false));keys.forget(null);
    }
    @Test public void aDamagedKeyIsNeverReplacedAndCannotLoseExistingDeletionAccess()throws Exception {
        File file=new File(folder.getRoot(),"deletion-key");Files.write(file.toPath(),"damaged".getBytes(StandardCharsets.US_ASCII));var keys=new LearningDeletionKey(file);
        try{keys.readOrCreate(true);fail("Damaged key was silently replaced");}catch(java.io.IOException expected){}
        try{keys.forget(null);fail("Damaged key was silently deleted");}catch(java.io.IOException expected){}
        assertEquals("damaged",new String(Files.readAllBytes(file.toPath()),StandardCharsets.US_ASCII));
    }
}
