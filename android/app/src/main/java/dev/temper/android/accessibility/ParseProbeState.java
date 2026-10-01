package dev.temper.android.accessibility;

import android.content.Context;
import android.os.SystemClock;
import dev.temper.android.adapters.*;
import org.json.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** One selected fictional conversation in RAM; exports diagnostics without message text/identity. */
public final class ParseProbeState {
    private static long deadline;
    private static VisibleConversation result;
    private static boolean repeatedSuppressed;
    private ParseProbeState(){}
    public static synchronized void arm(Context context){clear(context);deadline=SystemClock.elapsedRealtime()+60_000;}
    public static synchronized boolean armed(){if(deadline>0&&SystemClock.elapsedRealtime()>deadline)deadline=0;return deadline>0;}
    public static synchronized void save(Context context,VisibleConversation snapshot,boolean dedup){
        if(!armed())return;deadline=0;result=snapshot;repeatedSuppressed=dedup;
        try{export(context);}catch(Exception unavailable){result=VisibleConversation.unavailable(VisibleConversation.Status.UNSUPPORTED_LAYOUT);}
    }
    public static synchronized String status(){
        if(armed())return "Armed: open the fictional chat within one minute";
        if(result==null)return "Not armed";
        if(result.status()!=VisibleConversation.Status.AVAILABLE)return "Analysis unavailable: "+result.status();
        long local=result.turns().stream().filter(turn->turn.role()==VisibleConversation.Role.LOCAL).count();return "Parsed "+result.turns().size()+" visible turns: "+local+" local, "+(result.turns().size()-local)+" remote; repeat suppressed="+repeatedSuppressed;
    }
    public static synchronized VisibleConversation result(){return result;}
    public static synchronized void clear(Context context){deadline=0;result=null;repeatedSuppressed=false;new File(context.getFilesDir(),"whatsapp-parser-report.json").delete();}
    private static void export(Context context) throws Exception {
        JSONObject report=new JSONObject();report.put("status",result.status().name());report.put("repeatSuppressed",repeatedSuppressed);JSONArray roles=new JSONArray();for(var turn:result.turns())roles.put(turn.role().name());report.put("roles",roles);
        if(result.composer()!=null){var b=result.composer();report.put("composer",new JSONArray().put(b.left()).put(b.top()).put(b.right()).put(b.bottom()));}
        Files.write(new File(context.getFilesDir(),"whatsapp-parser-report.json").toPath(),report.toString().getBytes(StandardCharsets.UTF_8));
    }
}
