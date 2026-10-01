package dev.temper.android.privacy;

import android.content.Context;
import android.content.SharedPreferences;

/** Versioned, fail-closed permission independent of the Android system toggle. */
public final class ConsentStore {
    public static final String WHATSAPP="com.whatsapp";
    public static final int VERSION=2;
    private final SharedPreferences preferences;
    private final Context context;
    public ConsentStore(Context context){this(context,"MainActivity");}
    public ConsentStore(Context context,String name){this.context=context.getApplicationContext();preferences=context.getSharedPreferences(name,Context.MODE_PRIVATE);}
    public SharedPreferences preferences(){return preferences;}
    public boolean consented(){return preferences.getInt("consentVersion",0)==VERSION;}
    public boolean paused(){return preferences.getBoolean("paused",true);}
    public boolean allows(String packageName){return consented()&&!paused()&&WHATSAPP.equals(packageName);}
    private void clearProbe(){dev.temper.android.accessibility.ProbeState.clear();dev.temper.android.accessibility.LayoutMetadata.clear(context);dev.temper.android.accessibility.ParseProbeState.clear(context);dev.temper.android.accessibility.LiveCaptureState.clear();}
    public void accept(){clearProbe();preferences.edit().putInt("consentVersion",VERSION).putBoolean("paused",true).apply();}
    public void pause(boolean paused){preferences.edit().putBoolean("paused",paused).apply();if(paused)clearProbe();}
    public void revoke(){preferences.edit().remove("consentVersion").remove("liveConsentVersion").putBoolean("paused",true).apply();clearProbe();}
}
