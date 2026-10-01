package dev.temper.android;

import android.content.Context;
import dev.temper.android.adapters.*;
import dev.temper.android.api.LocalAnalysisClient;
import dev.temper.android.privacy.*;
import java.util.List;

/** Optional actual USB transport test using only generated text, never accessibility content. */
final class LiveTransportChecks {
    static void run(Context context)throws Exception{
        ConsentStore base=new ConsentStore(context);base.accept();base.pause(false);LiveConsent.accept(context);
        VisibleConversation snapshot=new VisibleConversation(VisibleConversation.Status.AVAILABLE,"a".repeat(64),List.of(
            new VisibleConversation.Turn("1".repeat(64),VisibleConversation.Role.LOCAL,"How did your fictional project go?"),
            new VisibleConversation.Turn("2".repeat(64),VisibleConversation.Role.REMOTE,"I am happy and excited! It was a wonderful success."),
            new VisibleConversation.Turn("3".repeat(64),VisibleConversation.Role.LOCAL,"Congratulations, I am glad it went well.")),new ScreenObservation.Bounds(168,2218,452,2315));
        var result=new LocalAnalysisClient().analyze(context,snapshot,()->true);if(!result.summary().available()||result.summary().spectrum().length!=8)throw new AssertionError("Actual USB model result unavailable");
        try{new LocalAnalysisClient().analyze(context,snapshot,()->false);throw new AssertionError("Stopped session could transmit");}catch(IllegalStateException expected){}
        LiveConsent.revoke(context);try{new LocalAnalysisClient().analyze(context,snapshot,()->true);throw new AssertionError("Revoked consent could transmit");}catch(IllegalStateException expected){}
    }
}
