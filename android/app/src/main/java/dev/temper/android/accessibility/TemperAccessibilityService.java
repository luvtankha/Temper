package dev.temper.android.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import dev.temper.android.privacy.ConsentStore;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;
import dev.temper.android.adapters.*;
import java.util.UUID;

/** Package detection and explicitly armed text-free layout calibration; never performs host actions. */
public final class TemperAccessibilityService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    private ConsentStore consent;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable probe=()->{
        if(consent==null||!consent.allows(ConsentStore.WHATSAPP)||(!ProbeState.armed()&&!ParseProbeState.armed()))return;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null)return;
        try{CharSequence pkg=root.getPackageName();if(pkg!=null&&ConsentStore.WHATSAPP.contentEquals(pkg)){
            if(ParseProbeState.armed()){
                VisibleConversation snapshot;
                boolean repeated=false;
                if(!supportedBuild())snapshot=VisibleConversation.unavailable(VisibleConversation.Status.UNSUPPORTED_LAYOUT);
                else{
                    WhatsAppAdapter adapter=new WhatsAppAdapter(UUID.randomUUID().toString());
                    snapshot=new WhatsAppTextReader().read(root,adapter);
                    VisibleSnapshotDeduplicator dedup=new VisibleSnapshotDeduplicator();
                    if(snapshot.status()==VisibleConversation.Status.AVAILABLE){
                        dedup.accept(snapshot);VisibleConversation second=new WhatsAppTextReader().read(root,adapter);
                        repeated=second.status()==VisibleConversation.Status.AVAILABLE&&!dedup.accept(second);
                        if(!repeated)snapshot=VisibleConversation.unavailable(VisibleConversation.Status.UNSUPPORTED_LAYOUT);
                    }
                }
                ParseProbeState.save(this,snapshot,repeated);
            }else ProbeState.save(new WhatsAppStructureProbe().read(root));
        }}
        catch(IllegalArgumentException unavailable){if(ParseProbeState.armed())ParseProbeState.save(this,VisibleConversation.unavailable(VisibleConversation.Status.UNSUPPORTED_LAYOUT),false);else ProbeState.fail();}
        finally{root.recycle();}
    };
    private boolean supportedBuild(){try{var info=getPackageManager().getPackageInfo(ConsentStore.WHATSAPP,0);long code=android.os.Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;return "2.26.37.73".equals(info.versionName)&&code==263707322;}catch(android.content.pm.PackageManager.NameNotFoundException missing){return false;}}
    private static volatile long lastSupportedEvent;
    public static boolean recentlyDetected(){long time=lastSupportedEvent;return time>0&&SystemClock.elapsedRealtime()-time<120_000;}
    private static void clear(){lastSupportedEvent=0;}
    @Override protected void onServiceConnected(){consent=new ConsentStore(this);consent.preferences().registerOnSharedPreferenceChangeListener(this);clear();if(!consent.consented())disableSelf();}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(consent==null||event==null)return;
        CharSequence name=event.getPackageName();
        if(name==null||!consent.allows(name.toString())){clear();return;}
        int type=event.getEventType();
        if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||type==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){lastSupportedEvent=SystemClock.elapsedRealtime();if(ProbeState.armed()||ParseProbeState.armed()){handler.removeCallbacks(probe);handler.postDelayed(probe,500);}}
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences preferences,String key){if(consent.paused()||!consent.consented()){clear();ProbeState.clear();ParseProbeState.clear(this);handler.removeCallbacks(probe);}if(!consent.consented())disableSelf();}
    @Override public void onInterrupt(){clear();ProbeState.clear();ParseProbeState.clear(this);handler.removeCallbacks(probe);}
    @Override public void onDestroy(){if(consent!=null)consent.preferences().unregisterOnSharedPreferenceChangeListener(this);clear();ProbeState.clear();ParseProbeState.clear(this);handler.removeCallbacks(probe);super.onDestroy();}
}
