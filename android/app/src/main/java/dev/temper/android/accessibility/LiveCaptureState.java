package dev.temper.android.accessibility;

import android.content.Context;
import android.os.SystemClock;
import dev.temper.android.privacy.AnalysisConsent;

/** One selected conversation session; only opaque identity and fixed status text survive here. */
public final class LiveCaptureState {
    private static long generation,deadline;
    private static String conversation;
    private static int window=-1;
    private static String status="Not started";
    private LiveCaptureState(){}
    public static synchronized boolean arm(Context context){clear();if(!AnalysisConsent.allowed(context)){status="Live processing consent required";return false;}deadline=SystemClock.elapsedRealtime()+60_000;status="Waiting for the selected chat";return true;}
    public static synchronized boolean armed(){if(deadline>0&&SystemClock.elapsedRealtime()>deadline){deadline=0;status="Selection expired; start again";}return deadline>0;}
    public static synchronized boolean active(){return conversation!=null;}
    public static synchronized long generation(){return generation;}
    public static synchronized boolean acceptsIdentity(String key,int id){return armed()||(conversation!=null&&conversation.equals(key)&&window==id);}
    public static synchronized boolean bind(String key,int id){if(armed()){deadline=0;conversation=key;window=id;return true;}return conversation!=null&&conversation.equals(key)&&window==id;}
    public static synchronized boolean current(long value){return generation==value&&active();}
    public static synchronized void status(long value,String message){if(generation==value)status=message;}
    public static synchronized String status(){armed();return status;}
    public static synchronized void clear(){dev.temper.android.learning.LearningConsent.SESSION.finish();generation++;deadline=0;conversation=null;window=-1;status="Stopped; visible context cleared";}
    public static synchronized void leave(){if(active())clear();}
    public static synchronized void unavailable(){clear();status="Analysis unavailable: supported plain-text chat required";}
}
