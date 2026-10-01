package dev.temper.android.overlay;

import dev.temper.android.adapters.ScreenObservation.Bounds;

public final class PopupPlacement {
    private PopupPlacement(){}
    public static Bounds place(Bounds viewport,Bounds character,int width,int height,int gap){
        if(!viewport.contains(character)||width<=0||height<=0||gap<0||width>viewport.right()-viewport.left()||height>viewport.bottom()-viewport.top())throw new IllegalArgumentException("Insufficient compact popup room");
        int left=clamp(character.left(),viewport.left(),viewport.right()-width);
        int above=character.top()-gap-height,below=character.bottom()+gap;
        if(above>=viewport.top())return new Bounds(left,above,left+width,above+height);
        if(below+height<=viewport.bottom())return new Bounds(left,below,left+width,below+height);
        int top=clamp(character.top(),viewport.top(),viewport.bottom()-height);
        if(character.left()-gap-width>=viewport.left())return new Bounds(character.left()-gap-width,top,character.left()-gap,top+height);
        if(character.right()+gap+width<=viewport.right())return new Bounds(character.right()+gap,top,character.right()+gap+width,top+height);
        // Very short chat areas can have no disjoint rectangle; keep the entire panel visible.
        top=clamp(above,viewport.top(),viewport.bottom()-height);
        return new Bounds(left,top,left+width,top+height);
    }
    private static int clamp(int value,int minimum,int maximum){return Math.max(minimum,Math.min(maximum,value));}
}
