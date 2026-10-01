package dev.temper.android.overlay;

/** Clamps a draggable companion to the usable display; fractions survive density/rotation changes. */
public final class FloatingPlacement {
    public record Position(int x,int y){}
    private FloatingPlacement(){}
    public static Position place(float x,float y,int width,int height,int characterWidth,int characterHeight,int top,int bottom){
        if(width<1||height<1||characterWidth<1||characterHeight<1||!Float.isFinite(x)||!Float.isFinite(y))throw new IllegalArgumentException("Invalid display");
        int maxX=Math.max(0,width-characterWidth),maxY=Math.max(top,height-bottom-characterHeight);
        return new Position(Math.round(Math.max(0,Math.min(1,x))*maxX),Math.max(top,Math.min(maxY,Math.round(Math.max(0,Math.min(1,y))*maxY))));
    }
}
