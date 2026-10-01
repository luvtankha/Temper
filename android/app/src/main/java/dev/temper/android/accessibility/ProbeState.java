package dev.temper.android.accessibility;

import android.os.SystemClock;
import dev.temper.android.adapters.ScreenObservation;

/** RAM only, no text. Arm expires after one minute and consumes only one chat layout. */
public final class ProbeState {
    private static long deadline;
    private static ScreenObservation observation;
    private static String status="Not armed";
    private ProbeState(){}
    public static synchronized void arm(){observation=null;deadline=SystemClock.elapsedRealtime()+60_000;status="Armed: open your fictional WhatsApp chat within one minute";}
    public static synchronized boolean armed(){if(deadline>0&&SystemClock.elapsedRealtime()>deadline){deadline=0;status="Expired; arm again";}return deadline>0;}
    public static synchronized void save(ScreenObservation screen){
        if(!armed())return;
        if(!dev.temper.android.privacy.ConsentStore.WHATSAPP.equals(screen.packageName())){status="Waiting for WhatsApp";return;}
        boolean composer=screen.nodes().stream().anyMatch(node->node.editable()&&"com.whatsapp:id/entry".equals(node.resourceId()));
        if(!composer){status="Waiting for supported chat composer";return;}
        observation=screen;deadline=0;status="Structure captured: "+screen.nodes().size()+" nodes; no text read";
    }
    public static synchronized void fail(){deadline=0;observation=null;status="Structure unavailable; no text read";}
    public static synchronized void clear(){deadline=0;observation=null;status="Not armed";}
    public static synchronized String status(){armed();return status;}
    public static synchronized ScreenObservation observation(){return observation;}
}
