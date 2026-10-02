package dev.temper.android.accessibility;

import dev.temper.android.privacy.ConsentStore;

/** Package-only foreground observations; input-method and overlay windows cannot end a chat. */
public final class ForegroundSessionPolicy {
    public enum Decision { READ_HOST, WAIT_FOR_HOST, END_SESSION }
    private boolean host,other,sensitive,unknown;
    public static boolean sensitivePackage(String packageName){
        return java.util.Set.of("android","com.android.systemui","com.android.settings","com.android.permissioncontroller","com.google.android.permissioncontroller","com.android.packageinstaller","com.google.android.packageinstaller").contains(packageName==null?"":packageName);
    }
    /** A focused system window can cover a still-active host without changing its root. */
    public void observeSystem(boolean active,boolean focused){if(active||focused)sensitive=true;}
    public void observe(boolean application,boolean active,boolean focused,String packageName){
        if(!application||(!active&&!focused))return;
        if(packageName==null||packageName.isBlank()){unknown=true;return;}
        if(sensitivePackage(packageName))sensitive=true;
        if(ConsentStore.WHATSAPP.equals(packageName))host=true;else other=true;
    }
    public Decision decision(boolean locked){
        if(locked||sensitive||other)return Decision.END_SESSION;
        if(unknown)return Decision.WAIT_FOR_HOST;
        return host?Decision.READ_HOST:Decision.WAIT_FOR_HOST;
    }
    /** Ordinary-app fallback uses package/window metadata only, never its content. */
    public boolean companionAllowed(boolean locked){return !locked&&!sensitive&&!unknown&&(host||other);}
}
