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
    // Android can replace an application window while showing or hiding the keyboard.
    // The verified header identity, rather than that transient window ID, selects the chat.
    public static synchronized boolean acceptsIdentity(String key,int id){return validIdentity(key,id)&&(armed()||(conversation!=null&&conversation.equals(key)));}
    public static synchronized boolean bind(String key,int id){
        if(!validIdentity(key,id))return false;
        if(armed()){deadline=0;conversation=key;}
        else if(conversation==null||!conversation.equals(key))return false;
        window=id;return true;
    }
    private static boolean validIdentity(String key,int id){return key!=null&&!key.isBlank()&&id>=0;}
    public static synchronized boolean current(long value){return generation==value&&active();}
    public static synchronized void status(long value,String message){if(generation==value)status=message;}
    public static synchronized String status(){armed();return status;}
    public static synchronized void clear(){dev.temper.android.learning.LearningConsent.SESSION.finish();generation++;deadline=0;conversation=null;window=-1;status="Stopped; visible context cleared";}
    public static synchronized void leave(){if(active())clear();}
    /** A layout gap clears estimates in the pipeline but does not consume the selected chat. */
    public static synchronized void unavailable(){status="Analysis waiting for readable text in the selected chat";}
    public static synchronized void changedConversation(){clear();status="Selected chat changed; start analysis in TEMPER";}
}
