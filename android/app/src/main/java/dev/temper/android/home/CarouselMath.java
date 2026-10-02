package dev.temper.android.home;

/** Geometry independent of artwork and catalog size. No per-frame catalog traversal. */
public final class CarouselMath {
    private CarouselMath(){}
    public static float clamp(float position,int count){if(count<1)throw new IllegalArgumentException("Empty catalog");if(!Float.isFinite(position))return 0;return Math.max(0,Math.min(count-1,position));}
    public static int nearest(float position,int count){return Math.round(clamp(position,count));}
    public static float scale(float distance){return Math.max(.48f,1-.28f*Math.abs(distance));}
    public static float opacity(float distance){return Math.max(.16f,1-.31f*Math.abs(distance));}
    public static int first(float position,int count){return Math.max(0,nearest(position,count)-3);}
    public static int last(float position,int count){return Math.min(count-1,nearest(position,count)+3);}
    public static int release(float position,float velocity,float stride,int count){return nearest(position-Math.max(-2,Math.min(2,velocity/stride*.12f)),count);}
}
