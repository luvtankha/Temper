package dev.temper.android;

import dev.temper.android.accessibility.ForegroundSessionPolicy;
import dev.temper.android.accessibility.LayoutRecovery;
import org.junit.Test;
import static org.junit.Assert.*;

public class SessionContinuityTests {
    @Test public void keyboardAndOverlayWindowsDoNotCloseForegroundChat(){
        var policy=new ForegroundSessionPolicy();
        policy.observe(true,true,false,"com.whatsapp");
        policy.observe(false,true,true,"com.google.android.inputmethod.latin");
        policy.observe(false,false,true,"dev.temper.android");
        assertEquals(ForegroundSessionPolicy.Decision.READ_HOST,policy.decision(false));
    }
    @Test public void missingRootsWaitButAnotherForegroundAppOrLockEndsSession(){
        var policy=new ForegroundSessionPolicy();
        policy.observe(true,false,false,"com.android.launcher");
        policy.observe(true,true,true,null);
        assertEquals(ForegroundSessionPolicy.Decision.WAIT_FOR_HOST,policy.decision(false));
        policy.observe(true,true,true,"com.whatsapp");
        assertEquals(ForegroundSessionPolicy.Decision.WAIT_FOR_HOST,policy.decision(false));
        policy=new ForegroundSessionPolicy();policy.observe(true,true,true,"com.whatsapp");
        assertEquals(ForegroundSessionPolicy.Decision.READ_HOST,policy.decision(false));
        assertEquals(ForegroundSessionPolicy.Decision.END_SESSION,policy.decision(true));
        policy.observe(true,false,true,"com.android.launcher");
        assertEquals(ForegroundSessionPolicy.Decision.END_SESSION,policy.decision(false));
    }
    @Test public void longUnreadableGapsRemainEligibleForSlowerAutomaticRecovery(){
        var recovery=new LayoutRecovery();
        assertFalse(recovery.waiting());
        assertEquals(400,recovery.delay(100));
        assertTrue(recovery.waiting());
        assertEquals(400,recovery.delay(1599));
        assertEquals(1500,recovery.delay(1600));
        assertEquals(1500,recovery.delay(86_400_100));
        recovery.reset();
        assertFalse(recovery.waiting());
        assertEquals(400,recovery.delay(86_400_200));
    }
    @Test public void focusedSystemSurfacesSuppressBackgroundHostAndCompanion(){
        var policy=new ForegroundSessionPolicy();policy.observe(true,true,false,"com.whatsapp");
        assertTrue(policy.companionAllowed(false));policy.observeSystem(false,true);
        assertEquals(ForegroundSessionPolicy.Decision.END_SESSION,policy.decision(false));assertFalse(policy.companionAllowed(false));
        for(String name:new String[]{"com.android.settings","com.android.systemui","com.android.permissioncontroller","com.google.android.packageinstaller"}){
            var sensitive=new ForegroundSessionPolicy();sensitive.observe(true,true,true,name);
            assertEquals(ForegroundSessionPolicy.Decision.END_SESSION,sensitive.decision(false));assertFalse(sensitive.companionAllowed(false));
        }
    }
    @Test public void ordinaryAppFallbackUsesOnlyKnownForegroundMetadata(){
        var ordinary=new ForegroundSessionPolicy();ordinary.observe(true,true,true,"dev.temper.fictionalapp");
        assertEquals(ForegroundSessionPolicy.Decision.END_SESSION,ordinary.decision(false));assertTrue(ordinary.companionAllowed(false));assertFalse(ordinary.companionAllowed(true));
        var unknown=new ForegroundSessionPolicy();unknown.observe(true,true,false,"com.whatsapp");unknown.observe(true,false,true,null);
        assertEquals(ForegroundSessionPolicy.Decision.WAIT_FOR_HOST,unknown.decision(false));assertFalse(unknown.companionAllowed(false));
        var keyboard=new ForegroundSessionPolicy();keyboard.observe(true,true,false,"com.whatsapp");keyboard.observe(false,true,true,"com.google.android.inputmethod.latin");
        assertEquals(ForegroundSessionPolicy.Decision.READ_HOST,keyboard.decision(false));assertTrue(keyboard.companionAllowed(false));
    }
}
