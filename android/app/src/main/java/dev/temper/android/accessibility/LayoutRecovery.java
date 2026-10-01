package dev.temper.android.accessibility;

/** A short grace period for moving layouts; no content is submitted while invalid. */
public final class LayoutRecovery {
    private long firstFailure=-1;
    public boolean retry(long now){
        if(firstFailure<0)firstFailure=now;
        return now-firstFailure<1500;
    }
    public void reset(){firstFailure=-1;}
}
