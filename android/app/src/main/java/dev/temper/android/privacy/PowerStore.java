package dev.temper.android.privacy;

import android.content.Context;

/** One power preference shared by the home button, companion notification and analysis service. */
public final class PowerStore {
    private final Context context;
    private final ConsentStore consent;
    public PowerStore(Context context){this.context=context.getApplicationContext();consent=new ConsentStore(this.context);}
    public boolean enabled(){return consent.preferences().getBoolean("powerEnabled",false)&&!consent.paused()&&AnalysisConsent.autoAccepted(context);}
    /** Explicit OFF cancels an ON waiting for old foreground-service teardown. */
    public long stopSequence(){return consent.preferences().getLong("powerStopSequence",0);}
    public void requestOff(){consent.preferences().edit().putLong("powerStopSequence",stopSequence()+1).apply();setEnabled(false);}
    public void setEnabled(boolean enabled){
        if(!enabled){consent.pause(true);return;}
        if(!AnalysisConsent.autoAccepted(context))throw new IllegalStateException("Automatic analysis permission is required");
        consent.clearProbe();
        consent.preferences().edit().putBoolean("powerEnabled",true).putBoolean("paused",false).apply();
    }
}
