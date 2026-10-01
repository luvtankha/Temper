package dev.temper.android.overlay;

/** Tracks only the character's gesture; canceled or multi-touch gestures never become taps. */
public final class DragGesture {
    public enum Finish { NONE, TAP, DRAG }
    public record Position(float x,float y){}
    private final float slop;
    private boolean active,dragging;
    private float downX,downY,startX,startY;
    public DragGesture(float slop){if(!Float.isFinite(slop)||slop<0)throw new IllegalArgumentException("Invalid touch slop");this.slop=slop;}
    public void begin(float rawX,float rawY,float x,float y){
        if(!Float.isFinite(rawX)||!Float.isFinite(rawY)||!Float.isFinite(x)||!Float.isFinite(y))throw new IllegalArgumentException("Invalid gesture coordinates");
        active=true;dragging=false;downX=rawX;downY=rawY;startX=x;startY=y;
    }
    public Position move(float rawX,float rawY){
        if(!active||!Float.isFinite(rawX)||!Float.isFinite(rawY))return null;
        float dx=rawX-downX,dy=rawY-downY;
        if(Math.hypot(dx,dy)>slop)dragging=true;
        return dragging?new Position(startX+dx,startY+dy):null;
    }
    public Finish finish(){Finish result=!active?Finish.NONE:dragging?Finish.DRAG:Finish.TAP;active=false;dragging=false;return result;}
    public boolean cancel(){boolean moved=active&&dragging;active=false;dragging=false;return moved;}
    public boolean dragging(){return active&&dragging;}
}
