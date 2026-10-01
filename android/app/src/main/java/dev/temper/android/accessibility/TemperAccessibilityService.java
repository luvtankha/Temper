package dev.temper.android.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import dev.temper.android.privacy.ConsentStore;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;

/** Package detection and explicitly armed text-free layout calibration; never performs host actions. */
public final class TemperAccessibilityService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    private ConsentStore consent;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable probe=()->{
        if(consent==null||!consent.allows(ConsentStore.WHATSAPP)||!ProbeState.armed())return;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null)return;
        try{CharSequence pkg=root.getPackageName();if(pkg!=null&&ConsentStore.WHATSAPP.contentEquals(pkg))ProbeState.save(new WhatsAppStructureProbe().read(root));}
        catch(IllegalArgumentException unavailable){ProbeState.fail();}
        finally{root.recycle();}
    };
    private static volatile long lastSupportedEvent;
    public static boolean recentlyDetected(){long time=lastSupportedEvent;return time>0&&SystemClock.elapsedRealtime()-time<120_000;}
    private static void clear(){lastSupportedEvent=0;}
    @Override protected void onServiceConnected(){consent=new ConsentStore(this);consent.preferences().registerOnSharedPreferenceChangeListener(this);clear();if(!consent.consented())disableSelf();}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(consent==null||event==null)return;
        CharSequence name=event.getPackageName();
        if(name==null||!consent.allows(name.toString())){clear();return;}
        int type=event.getEventType();
        if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||type==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){lastSupportedEvent=SystemClock.elapsedRealtime();if(ProbeState.armed()){handler.removeCallbacks(probe);handler.postDelayed(probe,500);}}
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences preferences,String key){if(consent.paused()||!consent.consented()){clear();ProbeState.clear();handler.removeCallbacks(probe);}if(!consent.consented())disableSelf();}
    @Override public void onInterrupt(){clear();ProbeState.clear();handler.removeCallbacks(probe);}
    @Override public void onDestroy(){if(consent!=null)consent.preferences().unregisterOnSharedPreferenceChangeListener(this);clear();ProbeState.clear();handler.removeCallbacks(probe);super.onDestroy();}
}
