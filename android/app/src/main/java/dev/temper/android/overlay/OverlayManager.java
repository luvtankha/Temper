package dev.temper.android.overlay;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import dev.temper.android.adapters.ScreenObservation.Bounds;
import dev.temper.android.character.CharacterView;
import dev.temper.android.character.Emotion;

/** Service-owned compact character; touch-through until the popup interaction is added. */
public final class OverlayManager {
    private final WindowManager windows;
    private final CharacterView character;
    private boolean attached;
    private Bounds placement;
    private static volatile String status="Hidden";
    public OverlayManager(AccessibilityService service){windows=(WindowManager)service.getSystemService(Context.WINDOW_SERVICE);character=new CharacterView(service);character.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
    public void setEmotion(Emotion emotion){character.setEmotion(emotion,true);}
    public static String status(){return status;}
    public boolean show(Bounds viewport,Bounds composer,float density){
        try{
            Bounds target=OverlayPlacement.place(viewport,composer,density);
            if(attached&&target.equals(placement))return true;
            WindowManager.LayoutParams params=new WindowManager.LayoutParams(target.right()-target.left(),target.bottom()-target.top(),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
            params.gravity=Gravity.TOP|Gravity.LEFT;params.x=target.left();params.y=target.top();
            if(android.os.Build.VERSION.SDK_INT>=30)params.setFitInsetsTypes(0);
            if(attached)windows.updateViewLayout(character,params);else{windows.addView(character,params);attached=true;}
            placement=target;status="Visible: compact character on composer edge";return true;
        }catch(RuntimeException unavailable){hide();status="Hidden: overlay unavailable";return false;}
    }
    public void hide(){if(attached){try{windows.removeViewImmediate(character);}catch(RuntimeException ignored){}attached=false;}placement=null;status="Hidden";}
}
