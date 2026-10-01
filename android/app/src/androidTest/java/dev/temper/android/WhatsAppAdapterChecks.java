package dev.temper.android;

import android.content.Context;
import dev.temper.android.adapters.*;
import dev.temper.android.accessibility.ParseProbeState;
import dev.temper.android.privacy.ConsentStore;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.File;
import org.json.*;

final class WhatsAppAdapterChecks {
    private static ScreenObservation.Bounds bounds(JSONArray a)throws Exception{return new ScreenObservation.Bounds(a.getInt(0),a.getInt(1),a.getInt(2),a.getInt(3));}
    private static ScreenObservation.Node text(ScreenObservation.Node n,String value){return new ScreenObservation.Node(n.parentIndex(),n.resourceId(),n.className(),n.bounds(),n.visible(),n.editable(),n.password(),value);}
    static void run(Context test,Context target)throws Exception{
        JSONObject fixture;try(var input=test.getAssets().open("whatsapp-layout-2.26.37.73.json")){fixture=new JSONObject(new String(input.readAllBytes(),StandardCharsets.UTF_8));}
        JSONArray raw=fixture.getJSONArray("nodes");List<ScreenObservation.Node> nodes=new ArrayList<>();
        for(int i=0;i<raw.length();i++){
            JSONObject n=raw.getJSONObject(i);String id=n.getString("id"),value=null;
            if(id.endsWith("/message_text"))value="Fictional test turn "+i;
            if(id.endsWith("/date"))value="10:00";
            if(id.endsWith("/conversation_contact_name"))value="Fictional test participant";
            if(id.endsWith("/entry"))value="SECRET DRAFT EXCLUDED";
            nodes.add(new ScreenObservation.Node(n.getInt("parent"),id,n.getString("class"),bounds(n.getJSONArray("bounds")),true,n.getBoolean("editable"),false,value));
        }
        ScreenObservation screen=new ScreenObservation("com.whatsapp",bounds(fixture.getJSONArray("viewport")),nodes);WhatsAppAdapter adapter=new WhatsAppAdapter("isolated-fixture-session");
        var result=adapter.read(screen);if(result.status()!=VisibleConversation.Status.AVAILABLE||result.turns().size()!=8)throw new AssertionError("Observed-layout fixture not parsed");
        long local=result.turns().stream().filter(t->t.role()==VisibleConversation.Role.LOCAL).count();if(local!=1)throw new AssertionError("Observed outgoing status role incorrect");
        if(!result.composer().equals(new ScreenObservation.Bounds(168,2218,452,2315)))throw new AssertionError("Observed composer mismatch");
        if(result.turns().stream().anyMatch(t->t.text().contains("SECRET")||t.text().endsWith("51")))throw new AssertionError("Draft or clipped row leaked");
        VisibleSnapshotDeduplicator dedup=new VisibleSnapshotDeduplicator();if(!dedup.accept(result)||dedup.accept(adapter.read(screen)))throw new AssertionError("Repeated window not deduplicated");
        var plan=adapter.plan(screen);int message=plan.rows().get(0).message();List<ScreenObservation.Node> changed=new ArrayList<>(nodes);changed.set(message,text(changed.get(message),"Changed fictional turn"));
        if(!dedup.accept(adapter.read(new ScreenObservation(screen.packageName(),screen.viewport(),changed))))throw new AssertionError("Changed content suppressed");
        changed=new ArrayList<>(nodes);changed.set(message,text(changed.get(message),"x".repeat(1000)));if(adapter.read(new ScreenObservation(screen.packageName(),screen.viewport(),changed)).status()!=VisibleConversation.Status.AVAILABLE)throw new AssertionError("Boundary length rejected");
        changed=new ArrayList<>(nodes);var original=changed.get(message);changed.set(message,new ScreenObservation.Node(original.parentIndex(),original.resourceId(),original.className(),new ScreenObservation.Bounds(200,original.bounds().top(),original.bounds().right(),original.bounds().bottom()),true,false,false,original.text()));
        if(adapter.read(new ScreenObservation(screen.packageName(),screen.viewport(),changed)).status()!=VisibleConversation.Status.AMBIGUOUS_ROLES)throw new AssertionError("Uncertain alignment accepted");
        changed=new ArrayList<>(nodes);var status=changed.get(23);changed.set(23,new ScreenObservation.Node(status.parentIndex(),"",status.className(),status.bounds(),true,false,false,null));
        if(adapter.read(new ScreenObservation(screen.packageName(),screen.viewport(),changed)).status()!=VisibleConversation.Status.AMBIGUOUS_ROLES)throw new AssertionError("Missing sender status guessed");
        changed=new ArrayList<>(nodes);var unknown=changed.get(19);changed.set(19,new ScreenObservation.Node(unknown.parentIndex(),"com.whatsapp:id/participant_name",unknown.className(),unknown.bounds(),true,false,false,"Fictional group sender"));
        if(adapter.read(new ScreenObservation(screen.packageName(),screen.viewport(),changed)).status()!=VisibleConversation.Status.UNSUPPORTED_LAYOUT)throw new AssertionError("Unknown group row accepted");
        if(adapter.read(new ScreenObservation("com.other",screen.viewport(),nodes)).status()!=VisibleConversation.Status.UNSUPPORTED_PACKAGE)throw new AssertionError("Unsupported package accepted");
        dedup.accept(VisibleConversation.unavailable(VisibleConversation.Status.NOT_CONVERSATION));if(!dedup.accept(result))throw new AssertionError("Unavailable did not reset dedup");
        try{
            ParseProbeState.arm(target);
            ParseProbeState.save(target,VisibleConversation.unavailable(VisibleConversation.Status.NOT_CONVERSATION),false);
            if(!ParseProbeState.armed()||ParseProbeState.result()!=null||new File(target.getFilesDir(),"whatsapp-parser-report.json").exists())throw new AssertionError("Home/transition screen consumed one-shot capture");
            ParseProbeState.save(target,result,true);
            if(ParseProbeState.armed())throw new AssertionError("Successful conversation did not consume one-shot capture");
            String report=new String(Files.readAllBytes(new File(target.getFilesDir(),"whatsapp-parser-report.json").toPath()),StandardCharsets.UTF_8);
            if(report.contains("Fictional")||report.contains("SECRET")||!new JSONObject(report).getBoolean("repeatSuppressed"))throw new AssertionError("Report leaked text or missed dedup");
            new ConsentStore(target,"test_adapter_clear").pause(true);if(ParseProbeState.result()!=null||new File(target.getFilesDir(),"whatsapp-parser-report.json").exists())throw new AssertionError("Pause did not clear parser");
        }finally{ParseProbeState.clear(target);target.getSharedPreferences("test_adapter_clear",0).edit().clear().commit();}
    }
}
