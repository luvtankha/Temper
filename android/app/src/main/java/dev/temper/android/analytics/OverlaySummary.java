package dev.temper.android.analytics;

import java.util.Objects;

/** Bounded presentation data; never contains captured chat text. */
public record OverlaySummary(String currentState,String direction,float[] spectrum,boolean available) {
    public OverlaySummary {
        Objects.requireNonNull(currentState);Objects.requireNonNull(direction);Objects.requireNonNull(spectrum);
        if(currentState.isBlank()||direction.isBlank()||currentState.length()>64||direction.length()>64||spectrum.length!=8)throw new IllegalArgumentException("Invalid compact summary");
        spectrum=spectrum.clone();for(float value:spectrum)if(!Float.isFinite(value)||value<0||value>1)throw new IllegalArgumentException("Invalid spectrum");
        if(!available)java.util.Arrays.fill(spectrum,0);
    }
    @Override public float[] spectrum(){return spectrum.clone();}
    public static OverlaySummary unavailable(){return new OverlaySummary("Analysis unavailable","Insufficient supported evidence",new float[8],false);}
    public static OverlaySummary analyzing(){return new OverlaySummary("Analyzing visible messages","Waiting for model estimates",new float[8],false);}
    public static OverlaySummary connectionUnavailable(){return new OverlaySummary("Computer connection unavailable","Check USB and local backend",new float[8],false);}
}
