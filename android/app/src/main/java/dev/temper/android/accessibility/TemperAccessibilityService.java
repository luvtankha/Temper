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
import dev.temper.android.overlay.OverlayManager;
import android.view.accessibility.AccessibilityWindowInfo;
import dev.temper.android.privacy.LiveConsent;
import dev.temper.android.analytics.OverlaySummary;
import dev.temper.android.character.Emotion;

/** Package detection and explicitly armed text-free layout calibration; never performs host actions. */
public final class TemperAccessibilityService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    private ConsentStore consent;
    private OverlayManager overlay;
    private int boundWindow=-1;
    private LivePipeline live;
    private final LayoutRecovery recovery=new LayoutRecovery();
    private final WhatsAppAdapter liveAdapter=new WhatsAppAdapter(UUID.randomUUID().toString());
    private final AdapterRegistry adapters=AdapterRegistry.whatsApp(liveAdapter);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable probe=()->{
        if(consent==null||!consent.allows(ConsentStore.WHATSAPP)){hideOverlay();return;}
        AccessibilityNodeInfo root=hostRoot();
        if(root==null){hideOverlay();return;}
        try{CharSequence pkg=root.getPackageName();if(pkg!=null&&ConsentStore.WHATSAPP.contentEquals(pkg)){
            if(LiveCaptureState.armed()||LiveCaptureState.active()){
                if(!LiveConsent.allowed(this)||!supportedBuild()){LiveCaptureState.clear();hideOverlay();return;}
                ScreenObservation structure=new WhatsAppStructureProbe().read(root);var anchor=liveAdapter.anchor(structure);
                if(anchor.status()!=VisibleConversation.Status.AVAILABLE){hideOverlay();return;}
                boundWindow=root.getWindowId();overlay.show(structure.viewport(),anchor.composer(),getResources().getDisplayMetrics().density);
                VisibleConversation snapshot=new WhatsAppTextReader().read(root,liveAdapter,key->LiveCaptureState.acceptsIdentity(key,boundWindow));
                if(snapshot.status()==VisibleConversation.Status.AVAILABLE){
                    var dedup=new VisibleSnapshotDeduplicator();dedup.accept(snapshot);var second=new WhatsAppTextReader().read(root,liveAdapter,key->LiveCaptureState.acceptsIdentity(key,boundWindow));
                    if(second.status()!=VisibleConversation.Status.AVAILABLE||dedup.accept(second))snapshot=VisibleConversation.unavailable(VisibleConversation.Status.UNSUPPORTED_LAYOUT);
                }
                if(snapshot.status()!=VisibleConversation.Status.AVAILABLE){unreadableLayout();return;}
                recovery.reset();
                if(!LiveCaptureState.bind(snapshot.conversationKey(),root.getWindowId())){hideOverlay();return;}
                live.submit(snapshot,result->{overlay.setEmotion(result.emotion());overlay.setSummary(result.summary());},summary->{overlay.setEmotion(Emotion.NEUTRAL);overlay.setSummary(summary);});
            }else if(ParseProbeState.armed()){
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
                ScreenObservation structure=new WhatsAppStructureProbe().read(root);
                var anchor=new WhatsAppAdapter("structural-anchor-session").anchor(structure);
                if(supportedBuild()&&anchor.status()==VisibleConversation.Status.AVAILABLE){boundWindow=root.getWindowId();overlay.show(structure.viewport(),anchor.composer(),getResources().getDisplayMetrics().density);}else hideOverlay();
            }else if(ProbeState.armed())ProbeState.save(new WhatsAppStructureProbe().read(root));
            else if(boundWindow==root.getWindowId()){
                ScreenObservation structure=new WhatsAppStructureProbe().read(root);var plan=new WhatsAppAdapter("structural-anchor-session").anchor(structure);
                if(plan.status()==VisibleConversation.Status.AVAILABLE)overlay.show(structure.viewport(),plan.composer(),getResources().getDisplayMetrics().density);else hideOverlay();
            }else hideOverlay();
        }}
        catch(IllegalArgumentException unavailable){if(LiveCaptureState.armed()||LiveCaptureState.active())unreadableLayout();else{hideOverlay();if(ParseProbeState.armed())ParseProbeState.save(this,VisibleConversation.unavailable(VisibleConversation.Status.UNSUPPORTED_LAYOUT),false);else if(ProbeState.armed())ProbeState.fail();}}
        finally{root.recycle();}
    };
    private void unavailable(){overlay.setEmotion(Emotion.NEUTRAL);overlay.setSummary(OverlaySummary.unavailable());}
    private void unreadableLayout(){
        live.clear();overlay.setEmotion(Emotion.NEUTRAL);
        if(recovery.retry(SystemClock.elapsedRealtime())){
            LiveCaptureState.status(LiveCaptureState.generation(),"Waiting for chat layout to settle");
            overlay.setSummary(new OverlaySummary("Chat layout changing","Waiting for readable text",new float[8],false));
            handler.removeCallbacks(probe);handler.postDelayed(probe,400);
        }else{
            LiveCaptureState.unavailable();recovery.reset();
            overlay.setSummary(new OverlaySummary("Unsupported chat layout","Restart live analysis in TEMPER",new float[8],false));
        }
    }
    private void hideOverlay(){recovery.reset();if(overlay!=null)overlay.hide();if(live!=null)live.clear();LiveCaptureState.leave();boundWindow=-1;ParseProbeState.forgetContent();}
    private AccessibilityNodeInfo hostRoot(){
        for(AccessibilityWindowInfo window:getWindows()){
            if(window.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION||(!window.isActive()&&!window.isFocused()))continue;
            AccessibilityNodeInfo root=window.getRoot();if(root==null)continue;
            CharSequence pkg=root.getPackageName();if(pkg!=null&&ConsentStore.WHATSAPP.contentEquals(pkg))return root;root.recycle();
        }return null;
    }
    private boolean supportedBuild(){try{var info=getPackageManager().getPackageInfo(ConsentStore.WHATSAPP,0);long code=android.os.Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;return adapters.resolve(ConsentStore.WHATSAPP,info.versionName,code).isPresent();}catch(android.content.pm.PackageManager.NameNotFoundException missing){return false;}}
    private static volatile long lastSupportedEvent;
    private static volatile boolean connected;
    public static boolean connected(){return connected;}
    public static boolean recentlyDetected(){long time=lastSupportedEvent;return time>0&&SystemClock.elapsedRealtime()-time<120_000;}
    private static void clear(){lastSupportedEvent=0;}
    @Override protected void onServiceConnected(){connected=true;consent=new ConsentStore(this);overlay=new OverlayManager(this);live=new LivePipeline(this);consent.preferences().registerOnSharedPreferenceChangeListener(this);clear();if(!consent.consented())disableSelf();}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(consent==null||event==null)return;
        CharSequence name=event.getPackageName();
        int type=event.getEventType();
        if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            if(name!=null&&ConsentStore.WHATSAPP.contentEquals(name)&&boundWindow>=0)hideOverlay();
            if(consent.paused()||!consent.consented()){hideOverlay();return;}
            handler.removeCallbacks(probe);handler.postDelayed(probe,150);
        }
        if(name==null||!consent.allows(name.toString())){clear();return;}
        if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||type==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED||type==AccessibilityEvent.TYPE_VIEW_SCROLLED){lastSupportedEvent=SystemClock.elapsedRealtime();if(ProbeState.armed()||ParseProbeState.armed()||LiveCaptureState.armed()||LiveCaptureState.active()||boundWindow>=0){handler.removeCallbacks(probe);handler.postDelayed(probe,type==AccessibilityEvent.TYPE_VIEW_SCROLLED?450:250);}}
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences preferences,String key){if(consent.paused()||!consent.consented()||("liveConsentVersion".equals(key)&&!LiveConsent.allowed(this))){hideOverlay();LiveCaptureState.clear();clear();ProbeState.clear();ParseProbeState.clear(this);handler.removeCallbacks(probe);}if(!consent.consented())disableSelf();}
    @Override public void onInterrupt(){hideOverlay();LiveCaptureState.clear();clear();ProbeState.clear();ParseProbeState.clear(this);handler.removeCallbacks(probe);}
    @Override public void onDestroy(){connected=false;if(consent!=null)consent.preferences().unregisterOnSharedPreferenceChangeListener(this);hideOverlay();LiveCaptureState.clear();if(live!=null)live.close();clear();ProbeState.clear();ParseProbeState.clear(this);handler.removeCallbacks(probe);super.onDestroy();}
}
