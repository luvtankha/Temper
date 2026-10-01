package dev.temper.android.privacy;

import android.content.Context;
import java.io.File;

/** Explicit cleanup of our inspection artifacts; never touches the host app or device account. */
public final class PrivacyControls {
    private PrivacyControls(){}
    public static void clearInspection(Context context){dev.temper.android.learning.LearningConsent.discard();new ConsentStore(context).pause(true);}
    public static void removeConnection(Context context){
        clearInspection(context);LiveConsent.revoke(context);
        File config=new File(context.getFilesDir(),"overlay-connection.json");
        if(config.exists()&&!config.delete())throw new IllegalStateException("USB configuration could not be removed");
    }
}
