package dev.temper.android.store;

import android.content.Context;
import android.content.SharedPreferences;
import dev.temper.android.BuildConfig;
import dev.temper.android.character.Avatar;

public final class PurchaseStore {
    private final SharedPreferences preferences;
    public PurchaseStore(Context context){preferences=context.getSharedPreferences("temper_purchases",Context.MODE_PRIVATE);}
    public boolean owned(Avatar avatar){return avatar.free()||EntitlementVerifier.valid(preferences.getString(avatar.productId(),null),BuildConfig.ENTITLEMENT_KEY,avatar.productId(),System.currentTimeMillis());}
    public void save(String product,String envelope){if(!EntitlementVerifier.valid(envelope,BuildConfig.ENTITLEMENT_KEY,product,System.currentTimeMillis()))throw new IllegalArgumentException("Ownership could not be verified");preferences.edit().putString(product,envelope).apply();}
    public void remove(String product){preferences.edit().remove(product).apply();}
}
