package dev.temper.android.character;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.function.Predicate;

/** Selection is cosmetic; a preference alone never grants a paid entitlement. */
public final class AvatarSelection {
    public static final String STORAGE="temper_avatar_selection";
    private final SharedPreferences preferences;
    private final Predicate<Avatar> owned;
    public AvatarSelection(Context context,Predicate<Avatar> owned){preferences=context.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);this.owned=owned;}
    public Avatar selected(){Avatar candidate=Avatar.fromId(preferences.getString("selected","alex"));return candidate.free()||owned.test(candidate)?candidate:Avatar.ALEX;}
    public void select(Avatar avatar){if(avatar==null||(!avatar.free()&&!owned.test(avatar)))throw new IllegalStateException("Purchase this companion before selecting it");if(!preferences.edit().putString("selected",avatar.id()).commit())throw new IllegalStateException("Selection could not be saved");}
}
