package dev.temper.android.overlay;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.WindowManager;
import dev.temper.android.adapters.ScreenObservation.Bounds;
import dev.temper.android.analytics.OverlaySummary;
import dev.temper.android.analytics.AnalyticsPanel;
import dev.temper.android.character.CharacterView;
import java.lang.reflect.Field;
import java.util.HashMap;

/** Generated gestures/windows over TEMPER's own dummy screen; no other app is inspected. */
public final class OverlayInteractionChecks {
    private OverlayInteractionChecks(){}
    public static void run(Context context){
        var preferences=context.getSharedPreferences("temper_analysis_overlay",Context.MODE_PRIVATE);var original=new HashMap<String,Object>(preferences.getAll());
        OverlayManager manager=null;
        try{
            preferences.edit().clear().commit();
            var windows=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);var display=windows.getMaximumWindowMetrics().getBounds();float density=context.getResources().getDisplayMetrics().density;
            Bounds viewport=new Bounds(display.left,display.top,display.right,display.bottom);
            int composerTop=display.bottom-Math.round(70*density),composerHeight=Math.round(32*density);
            Bounds composer=new Bounds(display.left+Math.round(56*density),composerTop,display.right-Math.round(32*density),composerTop+composerHeight);
            manager=new OverlayManager(context,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            if(!manager.show(viewport,composer,density))throw new AssertionError("Analysis character did not attach over dummy screen");
            CharacterView character=value(manager,"character",CharacterView.class);Bounds initial=value(manager,"placement",Bounds.class);
            float rawX=initial.left()+20,rawY=initial.top()+20;
            touch(character,MotionEvent.ACTION_DOWN,rawX,rawY);touch(character,MotionEvent.ACTION_MOVE,rawX+display.width()*.75f,rawY-200);touch(character,MotionEvent.ACTION_UP,rawX+display.width()*.75f,rawY-200);
            Bounds moved=value(manager,"placement",Bounds.class);
            if(moved.left()<=initial.left()||!preferences.getBoolean("positioned",false)||value(manager,"popupAttached",Boolean.class))throw new AssertionError("Dragging failed, lost preference or opened analytics");
            manager.setSummary(new OverlaySummary("Generated positive estimate","Generated stable direction",new float[]{.1f,.9f,.1f,.1f,.1f,.1f,.1f,.1f},true));manager.show(viewport,composer,density);
            if(!moved.equals(value(manager,"placement",Bounds.class)))throw new AssertionError("Analysis update reset dragged position");
            int keyboardTop=Math.round(display.height()*.55f);Bounds keyboardComposer=new Bounds(composer.left(),keyboardTop,composer.right(),keyboardTop+composerHeight);
            manager.show(viewport,keyboardComposer,density);if(value(manager,"placement",Bounds.class).bottom()>keyboardTop)throw new AssertionError("Dragged character blocked dummy composer");
            manager.show(viewport,composer,density);if(!moved.equals(value(manager,"placement",Bounds.class)))throw new AssertionError("Keyboard close lost original dragged position");
            touch(character,MotionEvent.ACTION_DOWN,100,100);touch(character,MotionEvent.ACTION_CANCEL,100,100);touch(character,MotionEvent.ACTION_UP,100,100);
            if(value(manager,"popupAttached",Boolean.class))throw new AssertionError("Canceled touch opened popup");
            touch(character,MotionEvent.ACTION_DOWN,100,100);touch(character,MotionEvent.ACTION_UP,100,100);if(!value(manager,"popupAttached",Boolean.class))throw new AssertionError("Tap no longer opens analytics");
            touch(character,MotionEvent.ACTION_DOWN,100,100);touch(character,MotionEvent.ACTION_MOVE,-10000,-10000);touch(character,MotionEvent.ACTION_CANCEL,-10000,-10000);touch(character,MotionEvent.ACTION_UP,-10000,-10000);
            Bounds top=value(manager,"placement",Bounds.class);if(!viewport.contains(top)||value(manager,"popupAttached",Boolean.class))throw new AssertionError("Canceled drag escaped viewport or opened popup");
            touch(character,MotionEvent.ACTION_DOWN,100,100);touch(character,MotionEvent.ACTION_UP,100,100);if(!value(manager,"popupAttached",Boolean.class))throw new AssertionError("Top-dragged character cannot open analytics");
            AnalyticsPanel panel=value(manager,"panel",AnalyticsPanel.class);var panelPosition=(WindowManager.LayoutParams)panel.getLayoutParams();
            if(panelPosition.height!=panel.getMeasuredHeight()||panelPosition.y+panelPosition.height>composer.top()||panelPosition.y<top.bottom())throw new AssertionError("Dragged popup clipped its graph or covered dummy input");
            manager.hide();manager=new OverlayManager(context,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);manager.show(viewport,composer,density);
            if(!top.equals(value(manager,"placement",Bounds.class)))throw new AssertionError("Service recreation lost saved character position");
        }finally{
            if(manager!=null)manager.hide();var editor=preferences.edit().clear();
            for(var entry:original.entrySet()){Object value=entry.getValue();if(value instanceof Float number)editor.putFloat(entry.getKey(),number);else if(value instanceof Boolean flag)editor.putBoolean(entry.getKey(),flag);}
            if(!editor.commit())throw new AssertionError("Could not restore character position preference");
        }
    }
    private static void touch(CharacterView character,int action,float rawX,float rawY){long now=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,action,rawX,rawY,0);try{character.dispatchTouchEvent(event);}finally{event.recycle();}}
    private static <T>T value(OverlayManager manager,String name,Class<T> type){try{Field field=OverlayManager.class.getDeclaredField(name);field.setAccessible(true);return type.cast(field.get(manager));}catch(ReflectiveOperationException failure){throw new AssertionError("Missing overlay test state",failure);}}
}
