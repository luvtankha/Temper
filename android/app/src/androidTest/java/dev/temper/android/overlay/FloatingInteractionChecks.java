package dev.temper.android.overlay;

import android.content.Context;
import android.view.WindowManager;
import dev.temper.android.adapters.ScreenObservation.Bounds;
import dev.temper.android.analytics.AnalyticsPanel;
import dev.temper.android.character.CharacterView;
import java.lang.reflect.Field;

/** Actual companion windows over the fictional fixture only; no host content is obtained. */
public final class FloatingInteractionChecks {
    private FloatingInteractionChecks(){}
    public static void run(Context context){
        FloatingOverlayService service=value(null,"instance",FloatingOverlayService.class);
        if(service==null||!FloatingOverlayService.running()||!FloatingOverlayService.visible())throw new AssertionError("Foreground companion is unavailable");
        CharacterView character=value(service,"character",CharacterView.class);
        character.performClick();
        if(!value(service,"popup",Boolean.class))throw new AssertionError("Companion tap did not show analytics");
        AnalyticsPanel panel=value(service,"panel",AnalyticsPanel.class);
        WindowManager.LayoutParams layout=(WindowManager.LayoutParams)panel.getLayoutParams();
        Bounds viewport=value(service,"popupViewport",Bounds.class);
        if(!viewport.contains(new Bounds(layout.x,layout.y,layout.x+layout.width,layout.y+layout.height)))throw new AssertionError("Companion analytics escaped usable bounds");
        character.performClick();
        if(value(service,"popup",Boolean.class))throw new AssertionError("Repeated companion tap created duplicate analytics");
        character.performClick();
        FloatingOverlayService.foregroundScreenAllowed(false);
        if(FloatingOverlayService.visible()||value(service,"popup",Boolean.class))throw new AssertionError("Sensitive-window metadata left a companion or analytics attached");
        if(!FloatingOverlayService.running())throw new AssertionError("Temporary sensitive window turned off the service");
        FloatingOverlayService.foregroundScreenAllowed(true);
        if(!FloatingOverlayService.visible()||value(service,"popup",Boolean.class))throw new AssertionError("Ordinary-window metadata did not restore the companion alone");
    }
    private static <T>T value(FloatingOverlayService service,String name,Class<T> type){try{Field field=FloatingOverlayService.class.getDeclaredField(name);field.setAccessible(true);return type.cast(field.get(service));}catch(ReflectiveOperationException failure){throw new AssertionError("Missing companion test state",failure);}}
}
