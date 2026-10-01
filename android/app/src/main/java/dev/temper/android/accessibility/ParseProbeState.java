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
    private static String summary;
    private static boolean waitingForConversation;
    private ParseProbeState(){}
    public static synchronized void arm(Context context){clear(context);deadline=SystemClock.elapsedRealtime()+60_000;waitingForConversation=true;}
    public static synchronized boolean armed(){if(deadline>0&&SystemClock.elapsedRealtime()>deadline)deadline=0;return deadline>0;}
    public static synchronized void save(Context context,VisibleConversation snapshot,boolean dedup){
        if(!armed())return;
        // Opening WhatsApp often delivers its home/transition screen before the selected chat.
        // No text was read for NOT_CONVERSATION; keep the explicit one-shot arm alive.
        if(snapshot.status()==VisibleConversation.Status.NOT_CONVERSATION){waitingForConversation=true;return;}
        deadline=0;waitingForConversation=false;result=snapshot;repeatedSuppressed=dedup;summary=null;
        try{export(context);}catch(Exception unavailable){result=VisibleConversation.unavailable(VisibleConversation.Status.UNSUPPORTED_LAYOUT);}
    }
    public static synchronized String status(){
        if(armed())return waitingForConversation?"Waiting for the fictional chat; open it within one minute":"Armed";
        if(waitingForConversation)return "Expired before a conversation was found; arm again";
        if(result==null)return summary==null?"Not armed":summary;
        if(result.status()!=VisibleConversation.Status.AVAILABLE)return "Analysis unavailable: "+result.status();
        long local=result.turns().stream().filter(turn->turn.role()==VisibleConversation.Role.LOCAL).count();return "Parsed "+result.turns().size()+" visible turns: "+local+" local, "+(result.turns().size()-local)+" remote; repeat suppressed="+repeatedSuppressed;
    }
    public static synchronized VisibleConversation result(){return result;}
    public static synchronized void forgetContent(){if(result!=null){summary=status()+"; content cleared";result=null;}}
    public static synchronized void clear(Context context){deadline=0;result=null;summary=null;waitingForConversation=false;repeatedSuppressed=false;new File(context.getFilesDir(),"whatsapp-parser-report.json").delete();}
    private static void export(Context context) throws Exception {
        JSONObject report=new JSONObject();report.put("status",result.status().name());report.put("repeatSuppressed",repeatedSuppressed);JSONArray roles=new JSONArray();for(var turn:result.turns())roles.put(turn.role().name());report.put("roles",roles);
        if(result.composer()!=null){var b=result.composer();report.put("composer",new JSONArray().put(b.left()).put(b.top()).put(b.right()).put(b.bottom()));}
        Files.write(new File(context.getFilesDir(),"whatsapp-parser-report.json").toPath(),report.toString().getBytes(StandardCharsets.UTF_8));
    }
}
