package dev.temper.android;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import dev.temper.android.accessibility.LiveCaptureState;
import dev.temper.android.api.LocalAnalysisClient;
import dev.temper.android.privacy.*;
import dev.temper.android.character.Emotion;

final class LiveChecks {
    static void run(Context target)throws Exception{
        if(dev.temper.android.learning.LearningConsent.SESSION.review()!=null)throw new AssertionError("Consent fixture must not replace pending user feedback");
        Context context=new ContextWrapper(target){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("test_live_checks_44_"+name,mode);}
        };
        ConsentStore base=new ConsentStore(context);base.preferences().edit().clear().commit();
        try{
        if(AnalysisConsent.allowed(context)||LiveCaptureState.arm(context))throw new AssertionError("Absent consent allowed analysis");
        base.accept();base.pause(false);
        if(LiveConsent.allowed(context)||AnalysisConsent.local(context)||AnalysisConsent.allowed(context)||LiveCaptureState.arm(context))throw new AssertionError("Base-only consent authorized local analysis or live transmission");
        LiveConsent.accept(context);
        if(!LiveConsent.allowed(context)||!LiveCaptureState.arm(context))throw new AssertionError("Explicit live consent not effective");
        LiveCaptureState.leave();if(!LiveCaptureState.armed())throw new AssertionError("App transition consumed selection arm");
        if(!LiveCaptureState.bind("a".repeat(64),7))throw new AssertionError("Selected chat did not bind");long generation=LiveCaptureState.generation();
        if(!LiveCaptureState.acceptsIdentity("a".repeat(64),7)||LiveCaptureState.acceptsIdentity("b".repeat(64),7)||!LiveCaptureState.acceptsIdentity("a".repeat(64),8)||LiveCaptureState.acceptsIdentity(null,8)||LiveCaptureState.acceptsIdentity("a".repeat(64),-1))throw new AssertionError("Pre-read selected identity gate incorrect");
        if(LiveCaptureState.bind("b".repeat(64),7)||!LiveCaptureState.bind("a".repeat(64),8)||!LiveCaptureState.current(generation))throw new AssertionError("Window replacement ended selection or crossed conversation boundary");
        var recovery=new dev.temper.android.accessibility.LayoutRecovery();
        if(recovery.delay(100)!=400||recovery.delay(1600)!=1500||recovery.delay(3_600_100)!=1500)throw new AssertionError("Long composer gap was not bounded for automatic retry");
        LiveCaptureState.unavailable();
        if(!LiveCaptureState.active()||!LiveCaptureState.current(generation)||!LiveCaptureState.bind("a".repeat(64),9))throw new AssertionError("Unreadable layout consumed selected conversation");
        recovery.reset();if(recovery.waiting()||recovery.delay(3_600_200)!=400)throw new AssertionError("Valid layout did not reset fast recovery");
        LiveCaptureState.leave();if(LiveCaptureState.current(generation)||LiveCaptureState.active())throw new AssertionError("Departure retained session");
        LiveCaptureState.arm(context);LiveCaptureState.bind("a".repeat(64),7);generation=LiveCaptureState.generation();LiveCaptureState.changedConversation();
        if(LiveCaptureState.current(generation)||LiveCaptureState.active()||LiveCaptureState.armed()||LiveCaptureState.acceptsIdentity("b".repeat(64),8))throw new AssertionError("Changed selected identity retained capture");
        LiveCaptureState.arm(context);LiveCaptureState.bind("a".repeat(64),7);base.pause(true);if(LiveConsent.allowed(context)||LiveCaptureState.active())throw new AssertionError("Pause retained capture");
        base.pause(false);LiveCaptureState.arm(context);LiveCaptureState.bind("a".repeat(64),7);LiveConsent.revoke(context);if(LiveCaptureState.active()||LiveConsent.allowed(context))throw new AssertionError("Revocation retained live session");
        AnalysisConsent.acceptLocal(context);base.pause(false);
        if(!AnalysisConsent.local(context)||!LiveCaptureState.arm(context)||!LiveCaptureState.bind("a".repeat(64),7))throw new AssertionError("Separate local consent did not authorize selected analysis");
        base.pause(true);if(AnalysisConsent.allowed(context)||LiveCaptureState.active())throw new AssertionError("Pause retained local analysis");
        base.pause(false);LiveCaptureState.arm(context);LiveCaptureState.bind("a".repeat(64),7);base.revoke();if(AnalysisConsent.allowed(context)||LiveCaptureState.active())throw new AssertionError("Revocation retained local analysis");
        String response="{\"available\":true,\"character\":\"HAPPY\",\"currentState\":\"Positive language\",\"direction\":\"Appears stable\",\"spectrum\":[0.1,0.7,0.1,0.1,0.1,0.1,0.1,0.1],\"trajectory\":\"STABLE\",\"evidenceStrength\":0.55}";
        var result=LocalAnalysisClient.parse(response);if(!result.summary().available()||result.emotion()!=Emotion.HAPPY||result.summary().spectrum()[1]!=.7f)throw new AssertionError("Response mapping incorrect");
        if(LocalAnalysisClient.parse("{\"available\":false}").summary().available())throw new AssertionError("Unavailable response invented values");
        try{LocalAnalysisClient.parse(response.replace("0.7","1.7"));throw new AssertionError("Invalid score accepted");}catch(IllegalArgumentException expected){}
        try{LocalAnalysisClient.parse(response.replace("STABLE","UNKNOWN"));throw new AssertionError("Unknown direction accepted");}catch(IllegalArgumentException expected){}
        }finally{LiveCaptureState.clear();base.preferences().edit().clear().commit();}
    }
}
