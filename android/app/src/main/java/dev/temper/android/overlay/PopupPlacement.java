package dev.temper.android.overlay;

import dev.temper.android.adapters.ScreenObservation.Bounds;

public final class PopupPlacement {
    private PopupPlacement(){}
    public static Bounds place(Bounds viewport,Bounds character,int width,int height,int gap){
        if(!viewport.contains(character)||width<=0||height<=0||gap<0||width>viewport.right()-viewport.left()||height+gap>character.top()-viewport.top())throw new IllegalArgumentException("Insufficient compact popup room");
        int left=Math.max(viewport.left(),Math.min(character.left(),viewport.right()-width));int bottom=character.top()-gap;
        return new Bounds(left,bottom-height,left+width,bottom);
    }
}
