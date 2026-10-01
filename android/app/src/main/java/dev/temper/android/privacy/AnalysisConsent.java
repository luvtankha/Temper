package dev.temper.android.privacy;

import android.content.Context;
import dev.temper.android.BuildConfig;

public final class AnalysisConsent {
    private AnalysisConsent(){}
    public static boolean local(Context context){ConsentStore base=new ConsentStore(context);return base.allows(ConsentStore.WHATSAPP)&&base.preferences().getInt("onDeviceConsentVersion",0)==1&&"LOCAL".equals(base.preferences().getString("analysisMode",""));}
    public static boolean allowed(Context context){return local(context)||(BuildConfig.DEBUG&&LiveConsent.allowed(context));}
    public static void acceptLocal(Context context){ConsentStore base=new ConsentStore(context);base.accept();base.preferences().edit().putInt("onDeviceConsentVersion",1).putString("analysisMode","LOCAL").remove("liveConsentVersion").apply();}
}
