package dev.temper.android.overlay;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import dev.temper.android.adapters.ScreenObservation.Bounds;
import dev.temper.android.character.CharacterView;
import dev.temper.android.character.Emotion;
import dev.temper.android.analytics.*;

/** Service-owned compact character: drag to move, tap for the compact estimates panel. */
public final class OverlayManager {
    private final WindowManager windows;
    private final CharacterView character;
    private boolean attached;
    private Bounds placement;
    private Bounds viewport;
    private Bounds composer;
    private float density;
    private final android.content.SharedPreferences preferences;
    private final DragGesture gesture;
    private boolean positioned;
    private float preferredX,preferredY;
    private final AnalyticsPanel panel;
    private boolean popupAttached;
    private boolean windowFailure;
    private long outsideDismissedAt;
    private static volatile String status="Hidden";
    private final Context context;
    private final int windowType;
    public OverlayManager(AccessibilityService service){this(service,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);}
    OverlayManager(Context service,int windowType){
        if(windowType!=WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY&&windowType!=WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)throw new IllegalArgumentException("Unsupported overlay type");
        this.windowType=windowType;
        context=service;
        windows=(WindowManager)service.getSystemService(Context.WINDOW_SERVICE);character=new CharacterView(service);panel=new AnalyticsPanel(service);
        preferences=service.getSharedPreferences("temper_analysis_overlay",Context.MODE_PRIVATE);
        positioned=preferences.getBoolean("positioned",false);preferredX=preferences.getFloat("x",0);preferredY=preferences.getFloat("y",0);
        if(!Float.isFinite(preferredX)||!Float.isFinite(preferredY)){positioned=false;preferredX=preferredY=0;preferences.edit().clear().apply();}
        gesture=new DragGesture(ViewConfiguration.get(service).getScaledTouchSlop());
        character.setOnClickListener(view->{if(popupAttached)dismissPopup();else if(android.os.SystemClock.elapsedRealtime()-outsideDismissedAt>350)showPopup();});
        character.setOnTouchListener((view,event)->{
            if(!attached||placement==null){gesture.cancel();return false;}
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN -> {gesture.begin(event.getRawX(),event.getRawY(),placement.left(),placement.top());return true;}
                case MotionEvent.ACTION_MOVE -> {if(event.getPointerCount()!=1){cancelGesture();return true;}moveGesture(event);return true;}
                case MotionEvent.ACTION_UP -> {moveGesture(event);DragGesture.Finish finished=gesture.finish();if(finished==DragGesture.Finish.DRAG)savePosition();else if(finished==DragGesture.Finish.TAP)view.performClick();return true;}
                case MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_POINTER_DOWN,MotionEvent.ACTION_POINTER_UP -> {cancelGesture();return true;}
                default -> {return true;}
            }
        });
        panel.setOnTouchListener((view,event)->{if(event.getAction()==MotionEvent.ACTION_OUTSIDE){outsideDismissedAt=android.os.SystemClock.elapsedRealtime();dismissPopup();return true;}return false;});
    }
    public void setEmotion(Emotion emotion){character.setEmotion(emotion,true);}
    public void setSummary(OverlaySummary summary){panel.bind(summary);if(popupAttached)showPopup();}
    public static String status(){return status;}
    public boolean windowFailure(){return windowFailure;}
    public boolean show(Bounds viewport,Bounds composer,float density){
        windowFailure=false;
        try{
            this.viewport=safeViewport(viewport,composer);this.composer=composer;this.density=density;
            Bounds target=positioned?DraggablePlacement.place(this.viewport,composer,density,preferredX,preferredY):OverlayPlacement.place(this.viewport,composer,density);
            character.setAvatar(new dev.temper.android.character.AvatarSelection(context,new dev.temper.android.store.PurchaseStore(context)::owned).selected());
            FloatingOverlayService.analysisVisible(true);
            if(!attached||!target.equals(placement))updateCharacter(target);
            status="Visible: drag character to move; tap for estimates";if(popupAttached)showPopup();return true;
        }catch(RuntimeException unavailable){hide();status="Hidden: overlay unavailable";return false;}
    }
    private Bounds safeViewport(Bounds viewport,Bounds composer){
        if(android.os.Build.VERSION.SDK_INT>=30){
            try{
                var metrics=windows.getMaximumWindowMetrics();var display=metrics.getBounds();var bars=metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                int left=Math.max(viewport.left(),display.left+bars.left),top=Math.max(viewport.top(),display.top+bars.top),right=Math.min(viewport.right(),display.right-bars.right);
                Bounds safe=new Bounds(left,top,right,viewport.bottom());
                if(safe.contains(composer))return safe;
            }catch(RuntimeException unavailable){/* The adapter's validated app bounds remain the fallback. */}
        }
        return viewport;
    }
    private void updateCharacter(Bounds target){
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(target.right()-target.left(),target.bottom()-target.top(),windowType,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        params.setTitle("TEMPER character");params.gravity=Gravity.TOP|Gravity.LEFT;params.x=target.left();params.y=target.top();
        if(android.os.Build.VERSION.SDK_INT>=30)params.setFitInsetsTypes(0);
        try{if(attached)windows.updateViewLayout(character,params);else{windows.addView(character,params);attached=true;}}
        catch(RuntimeException unavailable){windowFailure=true;throw unavailable;}
        placement=target;
    }
    private void moveGesture(MotionEvent event){
        DragGesture.Position next=gesture.move(event.getRawX(),event.getRawY());if(next==null)return;
        dismissPopup();
        try{
            Bounds target=DraggablePlacement.move(viewport,composer,density,next.x(),next.y());updateCharacter(target);
            var preference=DraggablePlacement.preference(viewport,target);preferredX=preference.x();preferredY=preference.y();positioned=true;
        }catch(RuntimeException unavailable){hide();status="Hidden: overlay unavailable";}
    }
    private void savePosition(){if(positioned)preferences.edit().putBoolean("positioned",true).putFloat("x",preferredX).putFloat("y",preferredY).apply();}
    private void cancelGesture(){if(gesture.cancel())savePosition();}
    private void showPopup(){
        if(!attached||placement==null||viewport==null||composer==null||gesture.dragging())return;
        try{
            Bounds chat=DraggablePlacement.chatViewport(viewport,composer);
            int width=Math.min(Math.round(280*density),chat.right()-chat.left());
            panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(chat.bottom()-chat.top(),View.MeasureSpec.AT_MOST));
            Bounds target=PopupPlacement.place(chat,placement,width,panel.getMeasuredHeight(),Math.round(4*density));
            WindowManager.LayoutParams params=new WindowManager.LayoutParams(target.right()-target.left(),target.bottom()-target.top(),windowType,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
            params.setTitle("TEMPER analytics");params.gravity=Gravity.TOP|Gravity.LEFT;params.x=target.left();params.y=target.top();if(android.os.Build.VERSION.SDK_INT>=30)params.setFitInsetsTypes(0);
            if(popupAttached)windows.updateViewLayout(panel,params);else{windows.addView(panel,params);popupAttached=true;}
        }catch(RuntimeException unavailable){dismissPopup();}
    }
    private void dismissPopup(){if(popupAttached){try{windows.removeViewImmediate(panel);}catch(RuntimeException ignored){}popupAttached=false;}}
    public void hide(){cancelGesture();dismissPopup();if(attached){try{windows.removeViewImmediate(character);}catch(RuntimeException ignored){}attached=false;}placement=null;viewport=null;composer=null;panel.bind(OverlaySummary.unavailable());character.setEmotion(Emotion.NEUTRAL,false);status="Hidden";FloatingOverlayService.analysisVisible(false);}
}
