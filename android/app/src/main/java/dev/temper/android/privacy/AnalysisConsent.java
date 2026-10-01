package dev.temper.android.privacy;

import android.content.Context;
import dev.temper.android.BuildConfig;

public final class AnalysisConsent {
    private AnalysisConsent(){}
    public static boolean local(Context context){ConsentStore base=new ConsentStore(context);return base.allows(ConsentStore.WHATSAPP)&&base.preferences().getInt("onDeviceConsentVersion",0)==1&&"LOCAL".equals(base.preferences().getString("analysisMode",""));}
    public static boolean allowed(Context context){return local(context)||(BuildConfig.DEBUG&&LiveConsent.allowed(context));}
    /** Saved opt-in for automatic visible-chat analysis, independent of the current power state. */
    public static boolean autoAccepted(Context context){ConsentStore base=new ConsentStore(context);return base.consented()&&base.preferences().getInt("onDeviceConsentVersion",0)==1&&"LOCAL".equals(base.preferences().getString("analysisMode",""))&&base.preferences().getInt("autoConsentVersion",0)==1;}
    public static boolean auto(Context context){return autoAccepted(context)&&local(context);}
    public static void acceptLocal(Context context){ConsentStore base=new ConsentStore(context);base.accept();base.preferences().edit().putInt("onDeviceConsentVersion",1).putString("analysisMode","LOCAL").remove("liveConsentVersion").apply();}
    public static void acceptAuto(Context context){acceptLocal(context);new ConsentStore(context).preferences().edit().putInt("autoConsentVersion",1).apply();}
}
