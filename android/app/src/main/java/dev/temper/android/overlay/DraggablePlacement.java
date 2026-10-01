package dev.temper.android.overlay;

import dev.temper.android.adapters.ScreenObservation.Bounds;

/** Saves a position against the full viewport; keyboard clamping does not replace that preference. */
public final class DraggablePlacement {
    public record Preference(float x,float y){}
    private DraggablePlacement(){}
    public static Bounds place(Bounds viewport,Bounds composer,float density,float x,float y){
        if(!Float.isFinite(x)||!Float.isFinite(y))throw new IllegalArgumentException("Invalid position preference");
        Bounds anchor=OverlayPlacement.place(viewport,composer,density);
        int width=anchor.right()-anchor.left(),height=anchor.bottom()-anchor.top();
        return move(viewport,composer,density,viewport.left()+fraction(x)*(viewport.right()-viewport.left()-width),viewport.top()+fraction(y)*(viewport.bottom()-viewport.top()-height));
    }
    public static Bounds move(Bounds viewport,Bounds composer,float density,float x,float y){
        if(!Float.isFinite(x)||!Float.isFinite(y))throw new IllegalArgumentException("Invalid drag coordinates");
        Bounds anchor=OverlayPlacement.place(viewport,composer,density);
        int width=anchor.right()-anchor.left(),height=anchor.bottom()-anchor.top();
        int left=Math.round(Math.max(viewport.left(),Math.min(viewport.right()-width,x)));
        int top=Math.round(Math.max(viewport.top(),Math.min(composer.top()-height,y)));
        return new Bounds(left,top,left+width,top+height);
    }
    public static Preference preference(Bounds viewport,Bounds placement){
        if(!viewport.contains(placement))throw new IllegalArgumentException("External character position");
        int width=placement.right()-placement.left(),height=placement.bottom()-placement.top();
        return new Preference((placement.left()-viewport.left())/(float)Math.max(1,viewport.right()-viewport.left()-width),(placement.top()-viewport.top())/(float)Math.max(1,viewport.bottom()-viewport.top()-height));
    }
    public static Bounds chatViewport(Bounds viewport,Bounds composer){
        if(!viewport.contains(composer)||composer.top()<=viewport.top())throw new IllegalArgumentException("Invalid chat viewport");
        return new Bounds(viewport.left(),viewport.top(),viewport.right(),composer.top());
    }
    private static float fraction(float value){return Math.max(0,Math.min(1,value));}
}
