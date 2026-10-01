package dev.temper.android.adapters;

/** Pure transformation boundary. Callers must enforce consent before observing a screen. */
public interface ChatPlatformAdapter {
    boolean supports(String packageName);
    VisibleConversation read(ScreenObservation screen);
}
