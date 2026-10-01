package dev.temper.android;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import dev.temper.android.auth.DeviceAccountStore;
import java.security.KeyStore;
import dev.temper.android.privacy.ConsentStore;
import android.widget.CheckBox;

/** Device smoke check for our own app only; never interacts with a host chat app. */
public final class FoundationSmokeInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    private Activity launch(){return startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    private View find(View root,String label){
        if(root instanceof TextView text&&label.contentEquals(text.getText()))return root;
        if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),label);if(found!=null)return found;}
        return null;
    }
    private void click(Activity activity,String label){View view=find(activity.getWindow().getDecorView(),label);if(!(view instanceof Button))throw new AssertionError("Missing button: "+label);view.performClick();}
    private dev.temper.android.character.CharacterView character(View view){
        if(view instanceof dev.temper.android.character.CharacterView c)return c;
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){var found=character(group.getChildAt(i));if(found!=null)return found;}return null;
    }
    private void restoreSetup(java.util.Map<String,?> original){
        var preferences=getTargetContext().getSharedPreferences("MainActivity",0);var edit=preferences.edit().clear();
        for(var item:original.entrySet()){
            Object value=item.getValue();if(value instanceof Boolean b)edit.putBoolean(item.getKey(),b);else if(value instanceof Integer n)edit.putInt(item.getKey(),n);else if(value instanceof String s)edit.putString(item.getKey(),s);else if(value instanceof Long n)edit.putLong(item.getKey(),n);else if(value instanceof Float n)edit.putFloat(item.getKey(),n);
        }
        if(!edit.commit())throw new AssertionError("Could not restore user's setup preferences");
    }
    @Override public void onStart(){
        Bundle result=new Bundle();
        java.util.Map<String,?> originalSetup=new java.util.HashMap<>(getTargetContext().getSharedPreferences("MainActivity",0).getAll());
        try{
            AdapterContractChecks.run();
            ProbeChecks.run(getTargetContext());
            WhatsAppAdapterChecks.run(getContext(),getTargetContext());
            OverlayPlacementChecks.run();
            runOnMainSync(()->{try{CharacterChecks.run(getTargetContext());AnalyticsChecks.run(getTargetContext());}catch(Exception failure){throw new RuntimeException(failure);}});
            getTargetContext().getSharedPreferences("MainActivity",0).edit().clear().commit();
            Activity first=launch();
            runOnMainSync(()->{
                if(find(first.getWindow().getDecorView(),"TEMPER")==null)throw new AssertionError("Onboarding missing");
                click(first,"Fictional preview");if(find(first.getWindow().getDecorView(),"Eight compact expressions")==null)throw new AssertionError("Preview missing");
            });
            waitForIdleSync();
            runOnMainSync(()->{
                var c=character(first.getWindow().getDecorView());if(c==null||!c.isAttachedToWindow())throw new AssertionError("Character preview not attached");
                c.setEmotion(dev.temper.android.character.Emotion.HAPPY,true);c.setEmotion(dev.temper.android.character.Emotion.SURPRISED,true);
            });
            android.os.SystemClock.sleep(400);waitForIdleSync();
            runOnMainSync(()->{
                var c=character(first.getWindow().getDecorView());if(c.transitioning()||c.emotion()!=dev.temper.android.character.Emotion.SURPRISED)throw new AssertionError("Interrupted transition did not settle");
                c.setEmotion(dev.temper.android.character.Emotion.SAD,true);click(first,"Back");if(c.transitioning())throw new AssertionError("Detached character kept animating");
                click(first,"Settings");Switch pause=(Switch)find(first.getWindow().getDecorView(),"Keep TEMPER paused");if(pause==null||!pause.isChecked())throw new AssertionError("Must default paused");pause.performClick();first.finish();
            });
            waitForIdleSync();Activity second=launch();
            runOnMainSync(()->{
                click(second,"Settings");Switch pause=(Switch)find(second.getWindow().getDecorView(),"Keep TEMPER paused");if(pause==null||pause.isChecked())throw new AssertionError("Pause preference did not persist");
                click(second,"Reset pause preference");Switch reset=(Switch)find(second.getWindow().getDecorView(),"Keep TEMPER paused");if(reset==null||!reset.isChecked())throw new AssertionError("Reset must restore paused");
                click(second,"Back");click(second,"Accessibility and consent");
                CheckBox agreement=(CheckBox)find(second.getWindow().getDecorView(),"I understand and opt in to selected WhatsApp test-chat inspection");
                View accept=find(second.getWindow().getDecorView(),"Save consent");
                if(agreement==null||agreement.isChecked()||accept==null||accept.isEnabled())throw new AssertionError("Consent must require unchecked opt-in");
                agreement.performClick();if(!accept.isEnabled())throw new AssertionError("Opt-in did not enable consent button");
                click(second,"Save consent");if(!new ConsentStore(getTargetContext()).consented()||!new ConsentStore(getTargetContext()).paused())throw new AssertionError("Accept must preserve pause");
                click(second,"Resume TEMPER");if(!new ConsentStore(getTargetContext()).allows(ConsentStore.WHATSAPP))throw new AssertionError("Resume not effective");
                click(second,"Revoke consent and disable");if(new ConsentStore(getTargetContext()).consented()||!new ConsentStore(getTargetContext()).paused())throw new AssertionError("Revoke must fail closed");second.finish();
            });
            ConsentStore consent=new ConsentStore(getTargetContext(),"test_consent_phase26");
            consent.preferences().edit().clear().commit();
            try{
                if(consent.allows(ConsentStore.WHATSAPP))throw new AssertionError("Default capture allowed");
                consent.pause(false);if(consent.allows(ConsentStore.WHATSAPP))throw new AssertionError("Resume bypassed missing consent");
                consent.accept();if(consent.allows(ConsentStore.WHATSAPP))throw new AssertionError("Accept did not default paused");
                consent.pause(false);if(!consent.allows(ConsentStore.WHATSAPP)||consent.allows("com.whatsapp.w4b")||consent.allows("com.other")||consent.allows(null))throw new AssertionError("Package gate incorrect");
                consent.preferences().edit().putInt("consentVersion",-1).commit();if(consent.allows(ConsentStore.WHATSAPP))throw new AssertionError("Stale consent accepted");
                consent.accept();consent.pause(false);consent.revoke();if(consent.allows(ConsentStore.WHATSAPP)||!consent.paused())throw new AssertionError("Revoke not safe");
            }finally{consent.preferences().edit().clear().commit();}
            String testStorage="test_auth_phase25";
            getTargetContext().getSharedPreferences(testStorage,0).edit().clear().commit();
            try{
                DeviceAccountStore accounts=new DeviceAccountStore(getTargetContext(),testStorage);
                char[] password="Fictional pass25!".toCharArray();accounts.create("foundation25_demo",password);
                for(char value:password)if(value!='\0')throw new AssertionError("Password buffer not cleared");
                if(!"foundation25_demo".equals(new DeviceAccountStore(getTargetContext(),testStorage).session()))throw new AssertionError("Session did not survive store recreation");
                String blob=getTargetContext().getSharedPreferences(testStorage,0).getString("encrypted","");if(blob.contains("foundation25_demo")||blob.contains("Fictional"))throw new AssertionError("Plaintext account leak");
                accounts.signOut();if(accounts.session()!=null)throw new AssertionError("Signout did not clear session");
                try{accounts.signIn("foundation25_demo","Wrong password!".toCharArray());throw new AssertionError("Wrong password accepted");}catch(IllegalArgumentException expected){}
                accounts.signIn("foundation25_demo","Fictional pass25!".toCharArray());if(accounts.session()==null)throw new AssertionError("Sign in failed");
                String damaged=blob.substring(0,blob.length()-4)+"AAAA";getTargetContext().getSharedPreferences(testStorage,0).edit().putString("encrypted",damaged).commit();
                try{accounts.session();throw new AssertionError("Modified ciphertext accepted");}catch(java.security.GeneralSecurityException expected){}
            }finally{getTargetContext().getSharedPreferences(testStorage,0).edit().clear().commit();KeyStore keyStore=KeyStore.getInstance("AndroidKeyStore");keyStore.load(null);keyStore.deleteEntry("dev.temper.auth."+testStorage);}
            restoreSetup(originalSetup);
            result.putString("stream","PASS: compact overlay grounding/density/keyboard/failure geometry; observed WhatsApp layout fixture/roles/clipping/dedup/uncertainty/privacy; one-shot probe/composer gate/text-free export/pause cleanup; adapter contract bounds/roles/immutability/failure/redaction; foundation UI/pause/reset; consent UI opt-in/resume/revoke and package/version gates; Keystore encrypted account, password rejection, sign-in/out, persistent session and tamper rejection; original setup preferences restored\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable failure){restoreSetup(originalSetup);result.putString("stream","FAIL: "+failure.getClass().getSimpleName()+": "+failure.getMessage());finish(Activity.RESULT_CANCELED,result);}
    }
}
