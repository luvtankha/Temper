package dev.temper.android;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import dev.temper.android.privacy.AnalysisConsent;
import dev.temper.android.privacy.ConsentStore;
import dev.temper.android.privacy.PowerStore;
import java.util.ArrayList;
import java.util.HashMap;

/** Own-app controls only: no model download, system setting, purchase or host chat is opened. */
final class PowerUiChecks {
    private PowerUiChecks(){}
    static void run(Activity activity){
        var preferences=activity.getSharedPreferences("MainActivity",0);var original=new HashMap<String,Object>(preferences.getAll());
        try{
            new PowerStore(activity).setEnabled(false);
            preferences.edit().clear().putInt("consentVersion",ConsentStore.VERSION).putInt("onDeviceConsentVersion",1).putString("analysisMode","LOCAL").putBoolean("paused",true).commit();
            home(activity);checkHome(activity,"ON","Avatar OFF");
            click(activity,"ON");if(find(activity,"Set up TEMPER")==null)throw new AssertionError("ON must guide missing setup");
            String consentLabel="I allow automatic on-phone analysis of supported visible chats while TEMPER is ON";
            View agreement=find(activity,consentLabel),save=find(activity,"Allow automatic analysis");
            if(!(agreement instanceof CheckBox check)||check.isChecked()||!(save instanceof Button)||save.isEnabled()||AnalysisConsent.autoAccepted(activity))throw new AssertionError("Earlier selected-chat consent silently granted automatic analysis");
            click(activity,"Back");checkHome(activity,"ON","Avatar OFF");
            if(AnalysisConsent.autoAccepted(activity))throw new AssertionError("Backing out saved automatic consent");
            click(activity,"ON");((CheckBox)find(activity,consentLabel)).performClick();if(!find(activity,"Allow automatic analysis").isEnabled())throw new AssertionError("Explicit opt-in did not enable consent action");
            click(activity,"Allow automatic analysis");if(!AnalysisConsent.autoAccepted(activity)||new PowerStore(activity).enabled())throw new AssertionError("Consent saving must leave power OFF");
            click(activity,"Back");checkHome(activity,"ON","Avatar OFF");
            // Controlled power fixture only; the foreground service is never started by this check.
            new PowerStore(activity).setEnabled(true);home(activity);checkHome(activity,"OFF","Avatar ON");View staleOff=find(activity,"OFF");new PowerStore(activity).setEnabled(false);staleOff.performClick();
            if(new PowerStore(activity).enabled()||find(activity,"Set up TEMPER")!=null)throw new AssertionError("A stale OFF label attempted to restart analysis");
            new PowerStore(activity).setEnabled(true);home(activity);click(activity,"OFF");
            if(new PowerStore(activity).enabled()||!new ConsentStore(activity).paused())throw new AssertionError("OFF did not pause and persist");checkHome(activity,"ON","Avatar OFF");
            field(activity,"powerStartPending",true);field(activity,"powerStartDeadline",android.os.SystemClock.elapsedRealtime()+5000);field(activity,"powerStartStopSequence",new PowerStore(activity).stopSequence());home(activity);
            View pending=find(activity,"Turning ON…");if(!(pending instanceof Button)||pending.isEnabled()||find(activity,"Avatar OFF")==null)throw new AssertionError("Pending control: "+pending+", enabled="+(pending!=null&&pending.isEnabled())+", OFF pill="+find(activity,"Avatar OFF")+", labels="+labels(activity));
            click(activity,"Settings");click(activity,"Turn TEMPER OFF");if((Boolean)field(activity,"powerStartPending")||new PowerStore(activity).enabled())throw new AssertionError("Explicit OFF did not cancel queued ON");
            field(activity,"powerStartPending",true);field(activity,"powerStartDeadline",android.os.SystemClock.elapsedRealtime()-1);field(activity,"powerStartStopSequence",new PowerStore(activity).stopSequence());invoke(activity,"pollPowerStart");
            if((Boolean)field(activity,"powerStartPending")||new PowerStore(activity).enabled()||find(activity,"Set up TEMPER")==null)throw new AssertionError("Start timeout did not leave OFF with setup guidance");
            field(activity,"powerStartPending",true);field(activity,"powerStartDeadline",android.os.SystemClock.elapsedRealtime()+5000);field(activity,"powerStartStopSequence",new PowerStore(activity).stopSequence());home(activity);
            long beforeStop=new PowerStore(activity).stopSequence();new PowerStore(activity).requestOff();if(new PowerStore(activity).stopSequence()==beforeStop)throw new AssertionError("Notification-style STOP did not record cancellation");invoke(activity,"pollPowerStart");
            if((Boolean)field(activity,"powerStartPending")||new PowerStore(activity).enabled()||find(activity,"Set up TEMPER")!=null||(Long)field(activity,"powerStartStopSequence")!=0)throw new AssertionError("Explicit STOP while power already OFF did not cancel pending ON before activation");checkHome(activity,"ON","Avatar OFF");
            field(activity,"powerStartPending",true);field(activity,"powerStartDeadline",android.os.SystemClock.elapsedRealtime()+5000);field(activity,"powerStartStopSequence",new PowerStore(activity).stopSequence());invoke(activity,"cancelPendingPowerOn");
            if((Boolean)field(activity,"powerStartPending")||new PowerStore(activity).enabled())throw new AssertionError("Activity cancellation retained pending power start");home(activity);
            click(activity,"Settings");if(find(activity,"Set up and permissions")==null||find(activity,"Fictional preview")==null||find(activity,"Rate analysis and manage feedback")==null)throw new AssertionError("Secondary tools missing from Settings");
            click(activity,"Back");checkHome(activity,"ON","Avatar OFF");
        }finally{
            invoke(activity,"cancelPendingPowerOn");
            new PowerStore(activity).setEnabled(false);var editor=preferences.edit().clear();
            for(var entry:original.entrySet()){Object value=entry.getValue();if(value instanceof Boolean flag)editor.putBoolean(entry.getKey(),flag);else if(value instanceof Integer number)editor.putInt(entry.getKey(),number);else if(value instanceof Long number)editor.putLong(entry.getKey(),number);else if(value instanceof Float number)editor.putFloat(entry.getKey(),number);else if(value instanceof String string)editor.putString(entry.getKey(),string);}
            if(!editor.commit())throw new AssertionError("Could not restore setup preferences");home(activity);
        }
    }
    private static java.util.List<String> labels(Activity activity){var names=new ArrayList<String>();buttons(activity.getWindow().getDecorView(),names);return names;}
    private static void checkHome(Activity activity,String action,String status){
        var buttons=new ArrayList<String>();buttons(activity.getWindow().getDecorView(),buttons);
        if(!buttons.equals(java.util.List.of(action))||find(activity,status)==null)throw new AssertionError("Home must have one master switch");
        View power=find(activity,action);if(!("Turn TEMPER "+(action.equals("ON")?"on":"off")).contentEquals(power.getContentDescription()))throw new AssertionError("Power button needs a clear accessibility action");
        if(find(activity,"Analyze my next WhatsApp chat")!=null||find(activity,"Analyze a chat privately")!=null||find(activity,"Rate analysis")!=null)throw new AssertionError("Legacy workflow remains on home");
    }
    private static void buttons(View view,ArrayList<String> labels){if(view instanceof Button button)labels.add(button.getText().toString());else if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)buttons(group.getChildAt(i),labels);}
    private static void click(Activity activity,String label){View view=find(activity,label);if(view==null||!view.isEnabled())throw new AssertionError("Missing enabled action: "+label);view.performClick();}
    private static View find(Activity activity,String label){return find(activity.getWindow().getDecorView(),label);}
    private static View find(View view,String label){if(view.getContentDescription()!=null&&label.contentEquals(view.getContentDescription()))return view;if(view instanceof TextView text&&label.contentEquals(text.getText()))return view;if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),label);if(found!=null)return found;}return null;}
    private static void home(Activity activity){invoke(activity,"home");}
    private static void invoke(Activity activity,String name){try{var method=MainActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(activity);}catch(ReflectiveOperationException failure){throw new AssertionError("Could not exercise power control: "+name,failure);}}
    private static Object field(Activity activity,String name){try{var field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}catch(ReflectiveOperationException failure){throw new AssertionError("Missing power start state",failure);}}
    private static void field(Activity activity,String name,Object value){try{var field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);field.set(activity,value);}catch(ReflectiveOperationException failure){throw new AssertionError("Missing power start state",failure);}}
}
