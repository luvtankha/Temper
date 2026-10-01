package dev.temper.android.overlay;

import dev.temper.android.adapters.ScreenObservation.Bounds;

/** Pixel placement derived from dp footprint, entirely above the input. */
public final class OverlayPlacement {
    private OverlayPlacement(){}
    public static Bounds place(Bounds viewport,Bounds composer,float density){
        if(!Float.isFinite(density)||density<=0||!viewport.contains(composer)||viewport.right()-viewport.left()>=viewport.bottom()-viewport.top())throw new IllegalArgumentException("Unsupported overlay geometry");
        int width=Math.round(64*density),height=Math.round(88*density),gap=Math.round(4*density);
        if(width>viewport.right()-viewport.left()||composer.top()-height<viewport.top())throw new IllegalArgumentException("Insufficient overlay room");
        int left=Math.max(viewport.left(),Math.min(composer.left()-width-gap,viewport.right()-width));
        return new Bounds(left,composer.top()-height,left+width,composer.top());
    }
}
