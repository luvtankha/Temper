package dev.temper.android.privacy;

import android.content.Context;
import android.content.SharedPreferences;

/** Versioned, fail-closed permission independent of the Android system toggle. */
public final class ConsentStore {
    public static final String WHATSAPP="com.whatsapp";
    public static final int VERSION=1;
    private final SharedPreferences preferences;
    public ConsentStore(Context context){this(context,"MainActivity");}
    public ConsentStore(Context context,String name){preferences=context.getSharedPreferences(name,Context.MODE_PRIVATE);}
    public SharedPreferences preferences(){return preferences;}
    public boolean consented(){return preferences.getInt("consentVersion",0)==VERSION;}
    public boolean paused(){return preferences.getBoolean("paused",true);}
    public boolean allows(String packageName){return consented()&&!paused()&&WHATSAPP.equals(packageName);}
    public void accept(){preferences.edit().putInt("consentVersion",VERSION).putBoolean("paused",true).apply();}
    public void pause(boolean paused){preferences.edit().putBoolean("paused",paused).apply();}
    public void revoke(){preferences.edit().remove("consentVersion").putBoolean("paused",true).apply();}
}
