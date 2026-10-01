package dev.temper.android.accessibility;

/** Automatic text processing needs its opt-in, saved ON state, local model and visible-service owner. */
public final class AutomaticCapturePolicy {
    private AutomaticCapturePolicy(){}
    public static boolean ready(boolean powerEnabled,boolean automaticConsent,boolean modelReady,boolean foregroundOwner){
        return powerEnabled&&automaticConsent&&modelReady&&foregroundOwner;
    }
}
