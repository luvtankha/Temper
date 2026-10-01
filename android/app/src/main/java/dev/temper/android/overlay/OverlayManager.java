package dev.temper.android.overlay;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import dev.temper.android.adapters.ScreenObservation.Bounds;
import dev.temper.android.character.CharacterView;
import dev.temper.android.character.Emotion;
import dev.temper.android.analytics.*;

/** Service-owned compact character; touch-through until the popup interaction is added. */
public final class OverlayManager {
    private final WindowManager windows;
    private final CharacterView character;
    private boolean attached;
    private Bounds placement;
    private Bounds viewport;
    private float density;
    private final AnalyticsPanel panel;
    private boolean popupAttached;
    private long outsideDismissedAt;
    private static volatile String status="Hidden";
    public OverlayManager(AccessibilityService service){
        windows=(WindowManager)service.getSystemService(Context.WINDOW_SERVICE);character=new CharacterView(service);panel=new AnalyticsPanel(service);
        character.setOnClickListener(view->{if(popupAttached)dismissPopup();else if(android.os.SystemClock.elapsedRealtime()-outsideDismissedAt>350)showPopup();});
        panel.setOnTouchListener((view,event)->{if(event.getAction()==MotionEvent.ACTION_OUTSIDE){outsideDismissedAt=android.os.SystemClock.elapsedRealtime();dismissPopup();return true;}return false;});
    }
    public void setEmotion(Emotion emotion){character.setEmotion(emotion,true);}
    public void setSummary(OverlaySummary summary){panel.bind(summary);if(popupAttached)showPopup();}
    public static String status(){return status;}
    public boolean show(Bounds viewport,Bounds composer,float density){
        try{
            this.viewport=viewport;this.density=density;
            Bounds target=OverlayPlacement.place(viewport,composer,density);
            if(attached&&target.equals(placement))return true;
            WindowManager.LayoutParams params=new WindowManager.LayoutParams(target.right()-target.left(),target.bottom()-target.top(),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
            params.setTitle("TEMPER character");
            params.gravity=Gravity.TOP|Gravity.LEFT;params.x=target.left();params.y=target.top();
            if(android.os.Build.VERSION.SDK_INT>=30)params.setFitInsetsTypes(0);
            if(attached)windows.updateViewLayout(character,params);else{windows.addView(character,params);attached=true;}
            placement=target;status="Visible: compact character on composer edge";if(popupAttached)showPopup();return true;
        }catch(RuntimeException unavailable){hide();status="Hidden: overlay unavailable";return false;}
    }
    private void showPopup(){
        if(!attached||placement==null||viewport==null)return;
        try{
            int width=Math.min(Math.round(280*density),viewport.right()-viewport.left());
            panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            Bounds target=PopupPlacement.place(viewport,placement,width,panel.getMeasuredHeight(),Math.round(4*density));
            WindowManager.LayoutParams params=new WindowManager.LayoutParams(width,panel.getMeasuredHeight(),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
            params.setTitle("TEMPER analytics");params.gravity=Gravity.TOP|Gravity.LEFT;params.x=target.left();params.y=target.top();if(android.os.Build.VERSION.SDK_INT>=30)params.setFitInsetsTypes(0);
            if(popupAttached)windows.updateViewLayout(panel,params);else{windows.addView(panel,params);popupAttached=true;}
        }catch(RuntimeException unavailable){dismissPopup();}
    }
    private void dismissPopup(){if(popupAttached){try{windows.removeViewImmediate(panel);}catch(RuntimeException ignored){}popupAttached=false;}}
    public void hide(){dismissPopup();if(attached){try{windows.removeViewImmediate(character);}catch(RuntimeException ignored){}attached=false;}placement=null;viewport=null;panel.bind(OverlaySummary.unavailable());character.setEmotion(Emotion.NEUTRAL,false);status="Hidden";}
}
