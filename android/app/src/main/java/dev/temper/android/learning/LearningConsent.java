package dev.temper.android.learning;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.*;
import java.io.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Base64;
import dev.temper.android.BuildConfig;

/** Research consent is independent of analysis permission, purchases and device identity. */
public final class LearningConsent {
    public static final int VERSION=1;
    public static final LearningSession SESSION=new LearningSession(SystemClock::elapsedRealtime);
    private static final Handler timer=new Handler(Looper.getMainLooper());
    private final Context context;private final SharedPreferences preferences;
    public LearningConsent(Context context){this.context=context.getApplicationContext();preferences=context.getSharedPreferences("temper_learning",0);}
    public boolean accepted(){return configured()&&preferences.getInt("version",0)==VERSION&&origin().equals(preferences.getString("origin",""));}
    public static String origin(){return BuildConfig.LEARNING_URL;}
    public static boolean configured(){try{java.net.URI url=new java.net.URI(origin());return "https".equals(url.getScheme())&&url.getHost()!=null&&url.getRawPath().isEmpty()&&url.getRawQuery()==null&&url.getRawFragment()==null&&url.getUserInfo()==null;}catch(Exception invalid){return false;}}
    public boolean canAccept(){return configured()&&(!tokenFile().exists()||origin().equals(storedOrigin()));}
    public void accept(){if(!canAccept())throw new IllegalStateException("Delete prior feedback before changing service");if(!preferences.edit().putInt("version",VERSION).putString("origin",origin()).commit())throw new IllegalStateException("Could not save permission");}
    public void decline(){revoke();}
    public void revoke(){preferences.edit().remove("version").commit();discard();}
    public static void discard(){timer.removeCallbacksAndMessages(null);SESSION.discard();}
    public void arm(){if(!accepted())throw new IllegalStateException("Contribution consent required");SESSION.arm();timer.removeCallbacksAndMessages(null);timer.postDelayed(SESSION::discard,LearningSession.TTL);}
    private File tokenFile(){return new File(context.getNoBackupFilesDir(),"learning-delete-token");}
    public synchronized String token(boolean create)throws Exception{File file=tokenFile();if(file.exists()){if(file.length()>60)throw new IOException("Invalid deletion key");String token=new String(Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.US_ASCII).strip();if(!token.matches("[A-Za-z0-9_-]{43}"))throw new IOException("Invalid deletion key");return token;}if(!create)return null;byte[] random=new byte[32];new SecureRandom().nextBytes(random);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);Files.write(file.toPath(),token.getBytes(java.nio.charset.StandardCharsets.US_ASCII),StandardOpenOption.CREATE_NEW);return token;}
    public String storedOrigin(){return preferences.getString("origin","");}
    public void forgetDeletedToken()throws Exception{Files.deleteIfExists(tokenFile().toPath());preferences.edit().clear().commit();}
}
