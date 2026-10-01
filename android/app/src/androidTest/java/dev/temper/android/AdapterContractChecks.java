package dev.temper.android;

import java.util.ArrayList;
import java.util.List;
import dev.temper.android.adapters.*;

final class AdapterContractChecks {
    static void run(){
        FakeChatPlatformAdapter adapter=new FakeChatPlatformAdapter();
        if(adapter.supports("com.whatsapp")||adapter.supports(null))throw new AssertionError("Fake must not handle real apps");
        ScreenObservation screen=new ScreenObservation(FakeChatPlatformAdapter.PACKAGE,new ScreenObservation.Bounds(0,0,360,800),List.of());
        VisibleConversation result=adapter.read(screen);
        if(result.status()!=VisibleConversation.Status.AVAILABLE||result.turns().get(0).role()!=VisibleConversation.Role.REMOTE||result.turns().get(1).role()!=VisibleConversation.Role.LOCAL||!screen.viewport().contains(result.composer()))throw new AssertionError("Fixture contract incomplete");
        if(!result.equals(adapter.read(screen)))throw new AssertionError("Fixture not deterministic");
        try{result.turns().clear();throw new AssertionError("Mutable turns");}catch(UnsupportedOperationException expected){}
        if(result.toString().contains("could")||result.turns().get(0).toString().contains("could"))throw new AssertionError("Debug text leak");
        if(adapter.read(new ScreenObservation("com.other",screen.viewport(),List.of())).status()!=VisibleConversation.Status.UNSUPPORTED_PACKAGE)throw new AssertionError("Package not fail closed");
        if(adapter.read(new ScreenObservation(FakeChatPlatformAdapter.PACKAGE,new ScreenObservation.Bounds(0,0,100,100),List.of())).status()!=VisibleConversation.Status.UNSUPPORTED_LAYOUT)throw new AssertionError("Bounds not fail closed");
        List<VisibleConversation.Turn> mutable=new ArrayList<>(result.turns());VisibleConversation copy=new VisibleConversation(result.status(),result.conversationKey(),mutable,result.composer());mutable.clear();if(copy.turns().size()!=2)throw new AssertionError("No defensive copy");
        try{new VisibleConversation(VisibleConversation.Status.NOT_CONVERSATION,"a".repeat(64),result.turns(),result.composer());throw new AssertionError("Failure leaked content");}catch(IllegalArgumentException expected){}
        try{new VisibleConversation.Turn("a".repeat(64),VisibleConversation.Role.REMOTE,"x".repeat(1001));throw new AssertionError("Unbounded text");}catch(IllegalArgumentException expected){}
        try{new ScreenObservation(screen.packageName(),screen.viewport(),List.of(new ScreenObservation.Node(0,"id","TextView",screen.viewport(),true,false,false,"private fixture")));throw new AssertionError("Cyclic parent accepted");}catch(IllegalArgumentException expected){}
    }
}
