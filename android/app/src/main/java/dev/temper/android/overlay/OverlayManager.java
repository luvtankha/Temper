package dev.temper.android.overlay;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import dev.temper.android.adapters.ScreenObservation.Bounds;

/** Service-owned, compact, touch-through placeholder; expression and popup arrive later. */
public final class OverlayManager {
    private final WindowManager windows;
    private final View character;
    private boolean attached;
    private Bounds placement;
    private static volatile String status="Hidden";
    public OverlayManager(AccessibilityService service){windows=(WindowManager)service.getSystemService(Context.WINDOW_SERVICE);character=new Placeholder(service);}
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
    private static final class Placeholder extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Placeholder(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        private void fill(Canvas c,int color,float left,float top,float right,float bottom){paint.setColor(color);c.drawOval(left,top,right,bottom,paint);}
        @Override protected void onDraw(Canvas canvas){
            canvas.save();canvas.scale(getWidth()/64f,getHeight()/88f);
            fill(canvas,0x55350c44,4,83,60,88); // tiny grounding shadow
            paint.setColor(0xff675078);canvas.drawRoundRect(18,48,46,76,9,9,paint);
            paint.setColor(0xff302139);canvas.drawRoundRect(17,73,29,86,4,4,paint);canvas.drawRoundRect(35,73,47,86,4,4,paint);
            fill(canvas,0xff35243b,10,6,54,50);fill(canvas,0xffffc894,13,13,51,52);
            paint.setColor(0xff35243b);canvas.drawArc(12,8,52,36,180,180,true,paint);
            fill(canvas,0xff332438,23,29,27,34);fill(canvas,0xff332438,37,29,41,34);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setColor(0xff8b4b43);canvas.drawArc(25,35,39,43,15,150,false,paint);paint.setStyle(Paint.Style.FILL);
            canvas.restore();
        }
    }
}
