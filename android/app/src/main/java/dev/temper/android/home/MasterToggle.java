package dev.temper.android.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

/** A single switch with a 112 x 60 dp target and explicit ON/OFF accessibility action. */
public final class MasterToggle extends Button {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress;
    private boolean checked;
    private ValueAnimator animation;
    public MasterToggle(Context context){super(context);setBackground(null);setTextColor(Color.TRANSPARENT);setPadding(0,0,0,0);setMinWidth(0);setMinHeight(0);}
    public void bind(boolean on,boolean pending){boolean changed=checked!=on;checked=on;setText(pending?"Turning ON…":on?"OFF":"ON");setEnabled(!pending);setContentDescription(pending?"Turning TEMPER on":on?"Turn TEMPER off":"Turn TEMPER on");if(android.os.Build.VERSION.SDK_INT>=30)setStateDescription(pending?"Waiting":on?"On":"Off");if(animation!=null)animation.cancel();if(changed&&isAttachedToWindow()&&ValueAnimator.areAnimatorsEnabled()){animation=ValueAnimator.ofFloat(progress,on?1:0);animation.setDuration(220);animation.addUpdateListener(a->{progress=(float)a.getAnimatedValue();invalidate();});animation.start();}else{progress=on?1:0;invalidate();}}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float d=getResources().getDisplayMetrics().density,w=92*d,h=44*d,x=(getWidth()-w)/2,y=(getHeight()-h)/2;paint.setStyle(Paint.Style.FILL);paint.setColor(blend(HomeTokens.ELEVATED,HomeTokens.ACCENT,progress));paint.setAlpha(isEnabled()?255:120);canvas.drawRoundRect(x,y,x+w,y+h,h/2,h/2,paint);paint.setColor(blend(HomeTokens.BORDER,HomeTokens.ACCENT,progress));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);canvas.drawRoundRect(x,y,x+w,y+h,h/2,h/2,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(HomeTokens.TEXT);float cx=x+22*d+progress*48*d;canvas.drawCircle(cx,y+h/2,17*d,paint);if(isFocused()){paint.setStyle(Paint.Style.STROKE);paint.setColor(HomeTokens.TEXT);canvas.drawRoundRect(x-4*d,y-4*d,x+w+4*d,y+h+4*d,h/2,h/2,paint);paint.setStyle(Paint.Style.FILL);}}
    private static int blend(int a,int b,float p){return Color.rgb(Math.round(Color.red(a)+(Color.red(b)-Color.red(a))*p),Math.round(Color.green(a)+(Color.green(b)-Color.green(a))*p),Math.round(Color.blue(a)+(Color.blue(b)-Color.blue(a))*p));}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Switch");info.setCheckable(true);info.setChecked(checked);}
    @Override protected void onDetachedFromWindow(){if(animation!=null){animation.cancel();animation=null;}super.onDetachedFromWindow();}
}
