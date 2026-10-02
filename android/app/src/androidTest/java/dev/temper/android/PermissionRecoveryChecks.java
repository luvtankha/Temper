package dev.temper.android;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.provider.Settings;
import dev.temper.android.privacy.AnalysisConsent;
import dev.temper.android.privacy.PowerStore;
import dev.temper.android.overlay.FloatingOverlayService;
import java.nio.file.Files;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/** Requires prepared QA weights and an external driver permitted to revoke the QA overlay AppOp. */
final class PermissionRecoveryChecks {
    static void run(Instrumentation test)throws Exception{
        var context=test.getTargetContext();if(!context.getPackageName().equals("dev.temper.android.qa"))throw new AssertionError("Permission fixture requires isolated QA");
        if(!dev.temper.android.inference.ModelFiles.ready(context))throw new AssertionError("Prepare the bundled offline model in the isolated QA app first");
        File ready=new File(context.getCacheDir(),"qa-overlay-ready");Files.deleteIfExists(ready.toPath());
        if(!Settings.canDrawOverlays(context))throw new AssertionError("QA AppOp must be allowed by the external test driver");
        Activity dummy=test.startActivitySync(new Intent(context,DebugOverlayActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{
            test.runOnMainSync(()->{AnalysisConsent.acceptAuto(context);new PowerStore(context).setEnabled(true);FloatingOverlayService.foregroundScreenAllowed(true);FloatingOverlayService.start(dummy);});
            await(test,()->FloatingOverlayService.running()&&FloatingOverlayService.visible(),5000,"QA overlay did not start");
            Files.write(ready.toPath(),"ready".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            await(test,()->!Settings.canDrawOverlays(context),20000,"External QA AppOp revocation not received");
            await(test,()->!new PowerStore(context).enabled()&&!FloatingOverlayService.running()&&!FloatingOverlayService.visible(),5000,"Revocation retained ON or overlay");
        }finally{test.runOnMainSync(()->{FloatingOverlayService.stop(context);dummy.finish();});Files.deleteIfExists(ready.toPath());}
    }
    private static void await(Instrumentation test,java.util.function.BooleanSupplier condition,long limit,String failure)throws Exception{
        long end=android.os.SystemClock.elapsedRealtime()+limit;AtomicBoolean value=new AtomicBoolean();do{test.runOnMainSync(()->value.set(condition.getAsBoolean()));if(value.get())return;Thread.sleep(25);}while(android.os.SystemClock.elapsedRealtime()<end);throw new AssertionError(failure);
    }
}
