package dev.temper.android;

import android.content.Context;
import dev.temper.android.accessibility.*;
import dev.temper.android.adapters.*;
import dev.temper.android.privacy.ConsentStore;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.List;

final class ProbeChecks {
    static void run(Context context) throws Exception {
        ScreenObservation.Bounds bounds=new ScreenObservation.Bounds(0,0,360,800);
        ScreenObservation.Node entry=new ScreenObservation.Node(-1,"com.whatsapp:id/entry","android.widget.EditText",new ScreenObservation.Bounds(10,700,350,770),true,true,false,"PRIVATE DRAFT MUST NEVER EXPORT");
        ScreenObservation screen=new ScreenObservation(ConsentStore.WHATSAPP,bounds,List.of(entry));
        ConsentStore consent=new ConsentStore(context,"test_probe_phase28");
        try{
            ProbeState.clear();ProbeState.save(screen);if(ProbeState.observation()!=null)throw new AssertionError("Probe captured without arm");
            ProbeState.arm();ProbeState.save(new ScreenObservation(ConsentStore.WHATSAPP,bounds,List.of()));if(ProbeState.observation()!=null||!ProbeState.armed())throw new AssertionError("Non-chat screen accepted");
            ProbeState.save(screen);if(ProbeState.observation()==null||ProbeState.armed())throw new AssertionError("One-shot composer capture failed");
            LayoutMetadata.export(context,screen);File file=new File(context.getFilesDir(),"whatsapp-layout-metadata.json");
            String metadata=new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);
            org.json.JSONObject exported=new org.json.JSONObject(metadata).getJSONArray("nodes").getJSONObject(0);
            if(metadata.contains("PRIVATE")||exported.has("text")||exported.has("description")||!"com.whatsapp:id/entry".equals(exported.getString("id")))throw new AssertionError("Metadata text leak or missing IDs");
            consent.pause(true);if(file.exists()||ProbeState.observation()!=null||ProbeState.armed())throw new AssertionError("Pause did not clear probe/export");
            ProbeState.arm();consent.revoke();if(ProbeState.armed())throw new AssertionError("Revocation did not cancel probe");
        }finally{ProbeState.clear();LayoutMetadata.clear(context);consent.preferences().edit().clear().commit();}
    }
}
