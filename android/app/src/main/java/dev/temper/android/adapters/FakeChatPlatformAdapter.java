package dev.temper.android.adapters;

import java.util.List;

/** Explicit fictional harness only. Never registered with the live AccessibilityService. */
public final class FakeChatPlatformAdapter implements ChatPlatformAdapter {
    public static final String PACKAGE="dev.temper.fixture";
    @Override public boolean supports(String packageName){return PACKAGE.equals(packageName);}
    @Override public VisibleConversation read(ScreenObservation screen){
        if(!supports(screen.packageName()))return VisibleConversation.unavailable(VisibleConversation.Status.UNSUPPORTED_PACKAGE);
        ScreenObservation.Bounds composer=new ScreenObservation.Bounds(16,700,344,760);
        if(!screen.viewport().contains(composer))return VisibleConversation.unavailable(VisibleConversation.Status.UNSUPPORTED_LAYOUT);
        return new VisibleConversation(VisibleConversation.Status.AVAILABLE,"a".repeat(64),List.of(
            new VisibleConversation.Turn("b".repeat(64),VisibleConversation.Role.REMOTE,"Fictional fixture: could we talk?"),
            new VisibleConversation.Turn("c".repeat(64),VisibleConversation.Role.LOCAL,"Fictional fixture: yes, let's talk.")),composer);
    }
}
