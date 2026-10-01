package dev.temper.android.character;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import java.util.Objects;

/** Original vector character. Feet remain fixed as the face and shoulders transition. */
public final class CharacterView extends View {
    public static final int WIDTH_DP=64, HEIGHT_DP=88;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private Emotion emotion=Emotion.NEUTRAL;
    private Avatar avatar=Avatar.ALEX;
    private float[] pose=emotion.pose();
    private ValueAnimator transition;
    public CharacterView(Context context){super(context);setContentDescription("Neutral companion");}
    public Emotion emotion(){return emotion;}
    public Avatar avatar(){return avatar;}
    @Override public boolean performClick(){return super.performClick();}
    public void setAvatar(Avatar value){avatar=Objects.requireNonNull(value);setContentDescription(avatar.displayName()+", "+emotion.label()+" companion");invalidate();}
    public boolean transitioning(){return transition!=null&&transition.isRunning();}
    public void setEmotion(Emotion next,boolean animate){
        Objects.requireNonNull(next);
        if(transition!=null){transition.cancel();transition=null;}
        emotion=next;setContentDescription(avatar.displayName()+", "+next.label()+" companion");
        float[] from=pose.clone(),to=next.pose();
        if(!animate||!isAttachedToWindow()||!ValueAnimator.areAnimatorsEnabled()){pose=to;invalidate();return;}
        transition=ValueAnimator.ofFloat(0,1);transition.setDuration(260);transition.setInterpolator(new DecelerateInterpolator());
        transition.addUpdateListener(animator->{float progress=(float)animator.getAnimatedValue();for(int i=0;i<pose.length;i++)pose[i]=from[i]+(to[i]-from[i])*progress;invalidate();});
        transition.start();
    }
    @Override protected void onDetachedFromWindow(){if(transition!=null){transition.cancel();transition=null;}pose=emotion.pose();super.onDetachedFromWindow();}
    private void oval(Canvas canvas,int color,float left,float top,float right,float bottom){paint.setStyle(Paint.Style.FILL);paint.setColor(color);canvas.drawOval(left,top,right,bottom,paint);}
    private void line(Canvas canvas,int color,float width,float x1,float y1,float x2,float y2){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(color);canvas.drawLine(x1,y1,x2,y2,paint);paint.setStyle(Paint.Style.FILL);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/64f,getHeight()/88f);
        oval(canvas,0x55350c44,4,83,60,88);
        paint.setColor(avatar.shoes());canvas.drawRoundRect(18,71,29,86,4,4,paint);canvas.drawRoundRect(35,71,46,86,4,4,paint);
        if(avatar==Avatar.NOVA)oval(canvas,avatar.hair(),9,7,55,68);
        paint.setColor(avatar.shirt());canvas.drawRoundRect(17,48+pose[3],47,75,9,9,paint);
        line(canvas,0xff564363,3,21,56+pose[3],20,69);line(canvas,0xff564363,3,43,56+pose[3],44,69);
        paint.setColor(avatar.skin());canvas.drawRoundRect(28,45,36,54,3,3,paint);
        canvas.save();canvas.rotate(pose[0],32,45);
        if(avatar==Avatar.ORBIT){paint.setColor(avatar.hair());path.reset();path.moveTo(11,24);path.lineTo(10,3);path.lineTo(25,13);path.close();path.moveTo(39,13);path.lineTo(54,3);path.lineTo(53,24);path.close();canvas.drawPath(path,paint);}
        if(avatar==Avatar.LUMA){line(canvas,0xffb4d7ec,2,32,6,32,12);oval(canvas,0xffed8dde,29,2,35,8);paint.setColor(avatar.hair());canvas.drawRoundRect(10,13,54,51,9,9,paint);paint.setColor(avatar.skin());canvas.drawRoundRect(14,17,50,47,7,7,paint);}
        else{oval(canvas,avatar.hair(),10,5,54,48);oval(canvas,avatar.skin(),12,15,52,51);}
        oval(canvas,avatar.skin(),10,29,16,38);oval(canvas,avatar.skin(),48,29,54,38);
        if(avatar!=Avatar.LUMA&&avatar!=Avatar.ORBIT){paint.setColor(avatar.hair());path.reset();path.moveTo(12,28);path.cubicTo(8,13,18,4,33,8);path.cubicTo(41,3,55,9,52,25);path.cubicTo(44,23,37,14,31,16);path.cubicTo(25,24,19,16,12,28);canvas.drawPath(path,paint);}
        if(avatar==Avatar.ORBIT){oval(canvas,0xffffecd5,18,35,46,48);oval(canvas,0xff553c3b,29,35,35,39);}
        line(canvas,0xff785044,2,19,13,29,10);
        float browY=25+pose[1],slope=pose[2];
        line(canvas,0xff503334,2.1f,20,browY+slope,28,browY-slope);
        line(canvas,0xff503334,2.1f,36,browY-slope,44,browY+slope);
        float eye=3.2f*pose[4];
        if(pose[5]>4){
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(0xff392c35);
            canvas.drawArc(20,30,29,36,190,160,false,paint);canvas.drawArc(35,30,44,36,190,160,false,paint);paint.setStyle(Paint.Style.FILL);
        }else{
            oval(canvas,0xffffffff,20,32-eye,29,32+eye);oval(canvas,0xffffffff,35,32-eye,44,32+eye);
            oval(canvas,0xff392c35,23,32-eye*.85f,27,32+eye*.85f);oval(canvas,0xff392c35,37,32-eye*.85f,41,32+eye*.85f);
        }
        line(canvas,0xffd7966b,1.3f,32,34,31,37);
        if(pose[6]>.15f){float half=6-Math.max(0,pose[6]-4)*.9f;oval(canvas,0xff713947,32-half,40,32+half,40+pose[6]);if(pose[5]>2)oval(canvas,0xfffff5ed,27,40,37,42);}
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setColor(0xff8e4a4c);
        if(pose[6]<5){path.reset();path.moveTo(25,42);path.quadTo(32,42+pose[5],39,42);canvas.drawPath(path,paint);}paint.setStyle(Paint.Style.FILL);
        canvas.restore();canvas.restore();
    }
}
