package dev.temper.android;

import android.content.Context;
import dev.temper.android.accessibility.LiveCaptureState;
import dev.temper.android.api.LocalAnalysisClient;
import dev.temper.android.privacy.*;
import dev.temper.android.character.Emotion;

final class LiveChecks {
    static void run(Context context)throws Exception{
        ConsentStore base=new ConsentStore(context);base.preferences().edit().remove("liveConsentVersion").commit();
        if(LiveConsent.allowed(context)||LiveCaptureState.arm(context))throw new AssertionError("Original consent allowed live transmission");
        base.accept();base.pause(false);LiveConsent.accept(context);
        if(!LiveConsent.allowed(context)||!LiveCaptureState.arm(context))throw new AssertionError("Explicit live consent not effective");
        LiveCaptureState.leave();if(!LiveCaptureState.armed())throw new AssertionError("App transition consumed selection arm");
        if(!LiveCaptureState.bind("a".repeat(64),7))throw new AssertionError("Selected chat did not bind");long generation=LiveCaptureState.generation();
        if(LiveCaptureState.bind("b".repeat(64),7)||LiveCaptureState.bind("a".repeat(64),8))throw new AssertionError("Session crossed conversation/window boundary");
        LiveCaptureState.leave();if(LiveCaptureState.current(generation)||LiveCaptureState.active())throw new AssertionError("Departure retained session");
        LiveCaptureState.arm(context);LiveCaptureState.bind("a".repeat(64),7);base.pause(true);if(LiveConsent.allowed(context)||LiveCaptureState.active())throw new AssertionError("Pause retained capture");
        base.pause(false);LiveCaptureState.arm(context);LiveCaptureState.bind("a".repeat(64),7);LiveConsent.revoke(context);if(LiveCaptureState.active()||LiveConsent.allowed(context))throw new AssertionError("Revocation retained live session");
        String response="{\"available\":true,\"character\":\"HAPPY\",\"currentState\":\"Positive language\",\"direction\":\"Appears stable\",\"spectrum\":[0.1,0.7,0.1,0.1,0.1,0.1,0.1,0.1],\"trajectory\":\"STABLE\",\"evidenceStrength\":0.55}";
        var result=LocalAnalysisClient.parse(response);if(!result.summary().available()||result.emotion()!=Emotion.HAPPY||result.summary().spectrum()[1]!=.7f)throw new AssertionError("Response mapping incorrect");
        if(LocalAnalysisClient.parse("{\"available\":false}").summary().available())throw new AssertionError("Unavailable response invented values");
        try{LocalAnalysisClient.parse(response.replace("0.7","1.7"));throw new AssertionError("Invalid score accepted");}catch(IllegalArgumentException expected){}
        try{LocalAnalysisClient.parse(response.replace("STABLE","UNKNOWN"));throw new AssertionError("Unknown direction accepted");}catch(IllegalArgumentException expected){}
    }
}
