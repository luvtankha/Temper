package dev.temper.android;

import dev.temper.android.adapters.*;
import java.util.*;

final class RegistryChecks {
    static void run(){
        var adapter=new WhatsAppAdapter("generated-registry-session");var registry=AdapterRegistry.whatsApp(adapter);
        if(registry.resolve("com.whatsapp","2.26.37.73",263707322).orElseThrow()!=adapter||registry.resolve("com.whatsapp","other",263707322).isPresent()||registry.resolve("com.whatsapp","2.26.37.73",1).isPresent()||registry.resolve("com.other","2.26.37.73",263707322).isPresent()||registry.resolve(null,null,0).isPresent())throw new AssertionError("Build profile gate incorrect");
        try{registry.profiles().clear();throw new AssertionError("Mutable registry");}catch(UnsupportedOperationException expected){}
        try{new AdapterRegistry(List.of(registry.profiles().get(0),registry.profiles().get(0)));throw new AssertionError("Duplicate build accepted");}catch(IllegalArgumentException expected){}
        ChatPlatformAdapter additional=new ChatPlatformAdapter(){public boolean supports(String name){return "example.test.chat".equals(name);}public VisibleConversation read(ScreenObservation screen){return VisibleConversation.unavailable(VisibleConversation.Status.NOT_CONVERSATION);}};
        var extended=new AdapterRegistry(List.of(registry.profiles().get(0),new AdapterRegistry.Profile("example.test.chat","1",1,additional)));
        if(extended.resolve("example.test.chat","1",1).orElseThrow()!=additional||extended.resolve("com.whatsapp","2.26.37.73",263707322).orElseThrow()!=adapter)throw new AssertionError("Extension changed existing profile");
        try{new AdapterRegistry.Profile("com.whatsapp","1",1,additional);throw new AssertionError("Unsupported profile accepted");}catch(IllegalArgumentException expected){}
    }
}
