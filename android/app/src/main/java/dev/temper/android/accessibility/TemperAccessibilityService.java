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
import dev.temper.android.privacy.AnalysisConsent;
import dev.temper.android.privacy.PowerStore;
import dev.temper.android.inference.ModelFiles;
import dev.temper.android.overlay.FloatingOverlayService;
import dev.temper.android.analytics.OverlaySummary;
import dev.temper.android.character.Emotion;
import android.app.KeyguardManager;

/** Consent-gated visible chat processing and text-free calibration; never performs host actions. */
public final class TemperAccessibilityService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    private ConsentStore consent;
    private OverlayManager overlay;
    private int boundWindow=-1;
    private LivePipeline live;
    private final LayoutRecovery recovery=new LayoutRecovery();
    private final WhatsAppAdapter liveAdapter=new WhatsAppAdapter(UUID.randomUUID().toString());
    private final AdapterRegistry adapters=AdapterRegistry.whatsApp(liveAdapter);
    private final ProbeDeadline deadline=new ProbeDeadline();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable probe=()->{
        deadline.reset();
        if(consent==null||!consent.allows(ConsentStore.WHATSAPP)){hideOverlay();return;}
        if(LiveCaptureState.automatic()&&!automaticReady()){hideOverlay();return;}
        if(automaticReady())LiveCaptureState.beginAutomatic(this);
        HostObservation host=hostRoot();
        if(host.decision()==ForegroundSessionPolicy.Decision.END_SESSION){hideOverlay();return;}
        AccessibilityNodeInfo root=host.root();
        if(root==null){overlay.hide();if(captureRequested())unreadableLayout();else hideOverlay();return;}
        try{CharSequence pkg=root.getPackageName();if(pkg!=null&&ConsentStore.WHATSAPP.contentEquals(pkg)){
            if(LiveCaptureState.armed()||LiveCaptureState.active()||LiveCaptureState.automatic()){
                if(!AnalysisConsent.allowed(this)||!supportedBuild()){LiveCaptureState.clear();hideOverlay();return;}
                ScreenObservation structure=new WhatsAppStructureProbe().read(root);var anchor=liveAdapter.anchor(structure);
                if(anchor.status()!=VisibleConversation.Status.AVAILABLE){unreadableLayout();return;}
                boundWindow=root.getWindowId();
                if(!overlay.show(structure.viewport(),anchor.composer(),getResources().getDisplayMetrics().density)){
                    if(overlay.windowFailure()){FloatingOverlayService.stop(this);LiveCaptureState.clear();hideOverlay();}
                    else unreadableLayout();
                    return;
                }
                boolean[] changedIdentity={false};
                boolean[] firstIdentity={true};
                java.util.function.Predicate<String> identityAllowed=key->{
                    if(firstIdentity[0]&&LiveCaptureState.automatic()){
                        if(!automaticReady())return false;
                        if(!LiveCaptureState.acceptsIdentity(key,boundWindow)){
                            // Select the new verified header before any of its body fields are read.
                            live.suspend();overlay.setEmotion(Emotion.NEUTRAL);overlay.setSummary(OverlaySummary.analyzing());
                            if(!LiveCaptureState.selectAutomatic(key,boundWindow))return false;
                        }
                    }
                    firstIdentity[0]=false;
                    boolean accepted=LiveCaptureState.acceptsIdentity(key,boundWindow);
                    if(!accepted&&LiveCaptureState.active())changedIdentity[0]=true;
                    return accepted;
                };
                VisibleConversation snapshot=new WhatsAppTextReader().read(root,liveAdapter,identityAllowed);
                if(snapshot.status()==VisibleConversation.Status.AVAILABLE){
                    var dedup=new VisibleSnapshotDeduplicator();dedup.accept(snapshot);var second=new WhatsAppTextReader().read(root,liveAdapter,identityAllowed);
                    if(second.status()!=VisibleConversation.Status.AVAILABLE||dedup.accept(second))snapshot=VisibleConversation.unavailable(VisibleConversation.Status.UNSUPPORTED_LAYOUT);
                }
                if(changedIdentity[0]){
                    if(LiveCaptureState.automatic()){
                        live.suspend();LiveCaptureState.clear();
                        if(automaticReady())LiveCaptureState.beginAutomatic(this);
                        unreadableLayout();
                    }else{LiveCaptureState.changedConversation();hideOverlay();}
                    return;
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
        catch(IllegalArgumentException unavailable){if(captureRequested())unreadableLayout();else{hideOverlay();if(ParseProbeState.armed())ParseProbeState.save(this,VisibleConversation.unavailable(VisibleConversation.Status.UNSUPPORTED_LAYOUT),false);else if(ProbeState.armed())ProbeState.fail();}}
        finally{root.recycle();}
    };
    private void unreadableLayout(){
        if(!recovery.waiting())live.suspend();
        overlay.setEmotion(Emotion.NEUTRAL);
        long now=SystemClock.elapsedRealtime();
        if(recovery.retry(now)){
            LiveCaptureState.status(LiveCaptureState.generation(),"Waiting for chat layout to settle");
            overlay.setSummary(new OverlaySummary("Chat layout changing","Waiting for readable text",new float[8],false));
        }else{
            LiveCaptureState.unavailable();
            overlay.setSummary(new OverlaySummary("Waiting for readable text","Analysis resumes automatically",new float[8],false));
        }
        if(captureRequested())scheduleProbe(recovery.delay(now));
    }
    private void scheduleProbe(long delay){long wait=deadline.schedule(SystemClock.elapsedRealtime(),delay);handler.removeCallbacks(probe);handler.postDelayed(probe,wait);}
    private void cancelProbe(){handler.removeCallbacks(probe);deadline.reset();}
    private boolean automaticReady(){return AutomaticCapturePolicy.ready(new PowerStore(this).enabled(),AnalysisConsent.auto(this),ModelFiles.ready(this),FloatingOverlayService.running());}
    private boolean captureRequested(){return LiveCaptureState.armed()||LiveCaptureState.active()||LiveCaptureState.automatic()||automaticReady();}
    private void hideOverlay(){cancelProbe();recovery.reset();if(overlay!=null)overlay.hide();if(live!=null){if(new PowerStore(this).enabled()&&AnalysisConsent.local(this))live.clearContext();else live.clear();}LiveCaptureState.leave();boundWindow=-1;ParseProbeState.forgetContent();}
    private record HostObservation(AccessibilityNodeInfo root,ForegroundSessionPolicy.Decision decision){}
    private HostObservation hostRoot(){
        ForegroundSessionPolicy policy=new ForegroundSessionPolicy();
        AccessibilityNodeInfo selected=null;
        for(AccessibilityWindowInfo window:getWindows()){
            try{
                if(window.getType()==AccessibilityWindowInfo.TYPE_SYSTEM){policy.observeSystem(window.isActive(),window.isFocused());continue;}
                if(window.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION||(!window.isActive()&&!window.isFocused()))continue;
                AccessibilityNodeInfo root=window.getRoot();if(root==null){policy.observe(true,window.isActive(),window.isFocused(),null);continue;}
                CharSequence pkg=root.getPackageName();String name=pkg==null?null:pkg.toString();
                policy.observe(true,window.isActive(),window.isFocused(),name);
                if(ConsentStore.WHATSAPP.equals(name)&&selected==null)selected=root;else root.recycle();
            }finally{window.recycle();}
        }
        KeyguardManager keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        boolean locked=keyguard!=null&&keyguard.isKeyguardLocked();
        var decision=policy.decision(locked);
        FloatingOverlayService.foregroundScreenAllowed(policy.companionAllowed(locked));
        if(decision!=ForegroundSessionPolicy.Decision.READ_HOST&&selected!=null){selected.recycle();selected=null;}
        return new HostObservation(selected,decision);
    }
    private boolean supportedBuild(){try{var info=getPackageManager().getPackageInfo(ConsentStore.WHATSAPP,0);long code=android.os.Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;return adapters.resolve(ConsentStore.WHATSAPP,info.versionName,code).isPresent();}catch(android.content.pm.PackageManager.NameNotFoundException missing){return false;}}
    private static volatile long lastSupportedEvent;
    private static volatile boolean connected;
    private static volatile TemperAccessibilityService instance;
    public static boolean connected(){return connected;}
    public static boolean recentlyDetected(){long time=lastSupportedEvent;return time>0&&SystemClock.elapsedRealtime()-time<120_000;}
    private static void clear(){lastSupportedEvent=0;}
    /** Called by the foreground companion after ON/OFF ownership changes; no host controls are touched. */
    public static void refreshPowerState(){TemperAccessibilityService service=instance;if(service!=null){if(service.automaticReady())service.live.warm();service.scheduleProbe(0);}}
    @Override protected void onServiceConnected(){connected=true;consent=new ConsentStore(this);overlay=new OverlayManager(this);live=new LivePipeline(this);consent.preferences().registerOnSharedPreferenceChangeListener(this);instance=this;clear();if(!consent.consented())disableSelf();else{if(automaticReady())live.warm();handler.post(probe);}}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(consent==null||event==null)return;
        CharSequence name=event.getPackageName();
        int type=event.getEventType();
        if(type==AccessibilityEvent.TYPE_WINDOWS_CHANGED){
            if(consent.paused()||!consent.consented()){hideOverlay();return;}
            // Window changes include shade/system focus changes and our own overlays.
            // Inspect only foreground metadata here; message parsing remains debounced.
            HostObservation host=hostRoot();if(host.root()!=null)host.root().recycle();
            if(host.decision()==ForegroundSessionPolicy.Decision.END_SESSION){hideOverlay();return;}
            if(host.decision()==ForegroundSessionPolicy.Decision.WAIT_FOR_HOST){overlay.hide();if(captureRequested())unreadableLayout();else hideOverlay();return;}
            if(ProbeState.armed()||ParseProbeState.armed()||captureRequested()||boundWindow>=0)scheduleProbe(150);
            return;
        }
        if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            if(consent.paused()||!consent.consented()){hideOverlay();return;}
            if(name==null||ForegroundSessionPolicy.sensitivePackage(name.toString())){FloatingOverlayService.foregroundScreenAllowed(false);hideOverlay();scheduleProbe(150);return;}
            // An old inference must not publish while a new host screen is awaiting
            // identity verification. Our own accessibility popup does not change the chat.
            if(LiveCaptureState.active()&&(name==null||!getPackageName().contentEquals(name)))unreadableLayout();
            scheduleProbe(150);
        }
        if(name==null||!consent.allows(name.toString())){clear();return;}
        if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||type==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED||type==AccessibilityEvent.TYPE_VIEW_SCROLLED){lastSupportedEvent=SystemClock.elapsedRealtime();if(ProbeState.armed()||ParseProbeState.armed()||captureRequested()||boundWindow>=0){scheduleProbe(type==AccessibilityEvent.TYPE_VIEW_SCROLLED?300:150);}}
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences preferences,String key){
        if(consent.paused()||!consent.consented()||(("liveConsentVersion".equals(key)||"onDeviceConsentVersion".equals(key)||"analysisMode".equals(key))&&!AnalysisConsent.allowed(this))){hideOverlay();LiveCaptureState.clear();clear();ProbeState.clear();ParseProbeState.clear(this);cancelProbe();}
        else if("powerEnabled".equals(key)||"autoConsentVersion".equals(key)){
            if(LiveCaptureState.automatic()&&!automaticReady())hideOverlay();
            scheduleProbe(0);
        }
        if(!consent.consented())disableSelf();
    }
    @Override public void onInterrupt(){hideOverlay();LiveCaptureState.clear();clear();ProbeState.clear();ParseProbeState.clear(this);cancelProbe();}
    @Override public void onDestroy(){connected=false;instance=null;FloatingOverlayService.foregroundScreenAllowed(false);FloatingOverlayService.stop(this);if(consent!=null)consent.preferences().unregisterOnSharedPreferenceChangeListener(this);hideOverlay();LiveCaptureState.clear();if(live!=null)live.close();clear();ProbeState.clear();ParseProbeState.clear(this);handler.removeCallbacks(probe);super.onDestroy();}
}
