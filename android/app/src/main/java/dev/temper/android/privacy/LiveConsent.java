package dev.temper.android.privacy;

import android.content.Context;

/** Separate consent: the original layout/parser opt-in does not authorize transmission. */
public final class LiveConsent {
    private LiveConsent(){}
    public static boolean allowed(Context context){ConsentStore base=new ConsentStore(context);return base.allows(ConsentStore.WHATSAPP)&&base.preferences().getInt("liveConsentVersion",0)==1;}
    public static void accept(Context context){new ConsentStore(context).preferences().edit().putInt("liveConsentVersion",1).apply();}
    public static void revoke(Context context){new ConsentStore(context).preferences().edit().remove("liveConsentVersion").apply();dev.temper.android.accessibility.LiveCaptureState.clear();}
}
