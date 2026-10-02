package dev.temper.android.overlay;

import dev.temper.android.adapters.ScreenObservation.Bounds;

/** Clamps a draggable companion to the usable display; fractions survive density/rotation changes. */
public final class FloatingPlacement {
    public record Position(int x,int y){}
    private FloatingPlacement(){}
    public static Position place(float x,float y,int width,int height,int characterWidth,int characterHeight,int top,int bottom){
        if(width<1||height<1||top<0||bottom<0||top>=height-bottom)throw new IllegalArgumentException("Invalid display");
        return place(x,y,new Bounds(0,top,width,height-bottom),characterWidth,characterHeight);
    }
    public static Position place(float x,float y,Bounds viewport,int characterWidth,int characterHeight){
        validate(viewport,characterWidth,characterHeight);
        if(!Float.isFinite(x)||!Float.isFinite(y))throw new IllegalArgumentException("Invalid position preference");
        return move(viewport.left()+fraction(x)*(viewport.right()-viewport.left()-characterWidth),viewport.top()+fraction(y)*(viewport.bottom()-viewport.top()-characterHeight),viewport,characterWidth,characterHeight);
    }
    public static Position move(float x,float y,Bounds viewport,int characterWidth,int characterHeight){
        validate(viewport,characterWidth,characterHeight);
        if(!Float.isFinite(x)||!Float.isFinite(y))throw new IllegalArgumentException("Invalid drag coordinates");
        return new Position(Math.round(Math.max(viewport.left(),Math.min(viewport.right()-characterWidth,x))),Math.round(Math.max(viewport.top(),Math.min(viewport.bottom()-characterHeight,y))));
    }
    private static void validate(Bounds viewport,int width,int height){
        if(viewport==null||width<1||height<1||width>viewport.right()-viewport.left()||height>viewport.bottom()-viewport.top())throw new IllegalArgumentException("Insufficient overlay room");
    }
    private static float fraction(float value){return Math.max(0,Math.min(1,value));}
}
