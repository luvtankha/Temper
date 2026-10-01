package dev.temper.android.adapters;

import java.util.*;
import dev.temper.android.privacy.ConsentStore;

/** Pure build-profile registry. Selection does not grant consent or enable observation. */
public final class AdapterRegistry {
    public record Profile(String packageName,String versionName,long versionCode,ChatPlatformAdapter adapter){
        public Profile{
            Objects.requireNonNull(packageName);Objects.requireNonNull(versionName);Objects.requireNonNull(adapter);
            if(packageName.isBlank()||versionName.isBlank()||versionCode<=0||!adapter.supports(packageName))throw new IllegalArgumentException("Invalid adapter profile");
        }
    }
    private final List<Profile> profiles;
    public AdapterRegistry(List<Profile> profiles){
        this.profiles=List.copyOf(profiles);Set<String> keys=new HashSet<>();
        for(Profile profile:this.profiles)if(!keys.add(profile.packageName()+"\u0000"+profile.versionName()+"\u0000"+profile.versionCode()))throw new IllegalArgumentException("Duplicate adapter build profile");
    }
    public static AdapterRegistry whatsApp(WhatsAppAdapter adapter){return new AdapterRegistry(List.of(new Profile(ConsentStore.WHATSAPP,"2.26.37.73",263707322,adapter)));}
    public Optional<ChatPlatformAdapter> resolve(String packageName,String versionName,long versionCode){
        return profiles.stream().filter(p->p.packageName().equals(packageName)&&p.versionName().equals(versionName)&&p.versionCode()==versionCode).map(Profile::adapter).findFirst();
    }
    public List<Profile> profiles(){return profiles;}
}
