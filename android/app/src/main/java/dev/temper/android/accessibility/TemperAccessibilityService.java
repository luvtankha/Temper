package dev.temper.android.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import dev.temper.android.privacy.ConsentStore;

/** Phase26 observes package metadata only. Never reads nodes/text or performs actions. */
public final class TemperAccessibilityService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    private ConsentStore consent;
    private static volatile long lastSupportedEvent;
    public static boolean recentlyDetected(){long time=lastSupportedEvent;return time>0&&SystemClock.elapsedRealtime()-time<120_000;}
    private static void clear(){lastSupportedEvent=0;}
    @Override protected void onServiceConnected(){consent=new ConsentStore(this);consent.preferences().registerOnSharedPreferenceChangeListener(this);clear();if(!consent.consented())disableSelf();}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(consent==null||event==null)return;
        CharSequence name=event.getPackageName();
        if(name==null||!consent.allows(name.toString())){clear();return;}
        int type=event.getEventType();
        if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||type==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)lastSupportedEvent=SystemClock.elapsedRealtime();
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences preferences,String key){if(consent.paused()||!consent.consented())clear();if(!consent.consented())disableSelf();}
    @Override public void onInterrupt(){clear();}
    @Override public void onDestroy(){if(consent!=null)consent.preferences().unregisterOnSharedPreferenceChangeListener(this);clear();super.onDestroy();}
}
