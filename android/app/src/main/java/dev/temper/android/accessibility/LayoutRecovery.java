package dev.temper.android.accessibility;

/** Bounded retry frequency for moving layouts; a long gap never stops the selected session. */
public final class LayoutRecovery {
    private long firstFailure=-1;
    public boolean waiting(){return firstFailure>=0;}
    public boolean retry(long now){
        if(firstFailure<0)firstFailure=now;
        return now-firstFailure<1500;
    }
    public long delay(long now){return retry(now)?400:1500;}
    public void reset(){firstFailure=-1;}
}
