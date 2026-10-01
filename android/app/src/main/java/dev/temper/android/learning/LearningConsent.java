package dev.temper.android.learning;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.*;
import java.io.*;
import dev.temper.android.BuildConfig;

/** Research consent is independent of analysis permission, purchases and device identity. */
public final class LearningConsent {
    public static final int VERSION=1;
    public static final LearningSession SESSION=new LearningSession(SystemClock::elapsedRealtime);
    private static final Handler timer=new Handler(Looper.getMainLooper());
    private static final Object KEY_LOCK=new Object();
    private final Context context;private final SharedPreferences preferences;
    public LearningConsent(Context context){this.context=context.getApplicationContext();preferences=context.getSharedPreferences("temper_learning",0);}
    public boolean accepted(){return configured()&&preferences.getInt("version",0)==VERSION&&origin().equals(preferences.getString("origin",""));}
    public static String origin(){return BuildConfig.LEARNING_URL;}
    public static boolean configured(){try{java.net.URI url=new java.net.URI(origin());return "https".equals(url.getScheme())&&url.getHost()!=null&&url.getRawPath().isEmpty()&&url.getRawQuery()==null&&url.getRawFragment()==null&&url.getUserInfo()==null;}catch(Exception invalid){return false;}}
    public boolean canAccept(){synchronized(KEY_LOCK){return !LearningOperations.SHARED.state().busy()&&configured()&&(!tokenFile().exists()||origin().equals(storedOrigin()));}}
    public void accept(){synchronized(KEY_LOCK){if(!canAccept())throw new IllegalStateException("Finish the current request and delete prior feedback before changing service");if(!preferences.edit().putInt("version",VERSION).putString("origin",origin()).commit())throw new IllegalStateException("Could not save permission");}}
    public void decline(){revoke();}
    public void revoke(){preferences.edit().remove("version").commit();discard();}
    public static synchronized void discard(){timer.removeCallbacksAndMessages(null);SESSION.discard();}
    public static synchronized void discard(String expectedId){if(SESSION.discard(expectedId))timer.removeCallbacksAndMessages(null);}
    public void arm(){synchronized(LearningConsent.class){if(!accepted()||LearningOperations.SHARED.state().busy())throw new IllegalStateException("Contribution consent required and no request may be running");SESSION.arm();String id=SESSION.review().id();timer.removeCallbacksAndMessages(null);timer.postDelayed(()->discard(id),LearningSession.TTL);}}
    private File tokenFile(){return new File(context.getNoBackupFilesDir(),"learning-delete-token");}
    public String token(boolean create)throws Exception{synchronized(KEY_LOCK){return new LearningDeletionKey(tokenFile()).readOrCreate(create);}}
    public String storedOrigin(){return preferences.getString("origin","");}
    public void forgetDeletedToken(String expectedToken)throws Exception{synchronized(KEY_LOCK){new LearningDeletionKey(tokenFile()).forget(expectedToken);if(!preferences.edit().clear().commit())throw new IOException("Could not clear feedback permission");}}
}
