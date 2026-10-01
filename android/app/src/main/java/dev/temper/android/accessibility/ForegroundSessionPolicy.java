package dev.temper.android.accessibility;

import dev.temper.android.privacy.ConsentStore;

/** Package-only foreground observations; input-method and overlay windows cannot end a chat. */
public final class ForegroundSessionPolicy {
    public enum Decision { READ_HOST, WAIT_FOR_HOST, END_SESSION }
    private boolean host,other;
    public void observe(boolean application,boolean active,boolean focused,String packageName){
        if(!application||(!active&&!focused)||packageName==null||packageName.isBlank())return;
        if(ConsentStore.WHATSAPP.equals(packageName))host=true;else other=true;
    }
    public Decision decision(boolean locked){
        if(locked||other)return Decision.END_SESSION;
        return host?Decision.READ_HOST:Decision.WAIT_FOR_HOST;
    }
}
