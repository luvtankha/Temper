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
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            getTargetContext().getSharedPreferences("MainActivity",0).edit().clear().commit();
            Activity first=launch();
            runOnMainSync(()->{
                if(find(first.getWindow().getDecorView(),"TEMPER")==null)throw new AssertionError("Onboarding missing");
                click(first,"Fictional preview");if(find(first.getWindow().getDecorView(),"Neutral companion placeholder")==null)throw new AssertionError("Preview missing");click(first,"Back");
                click(first,"Settings");Switch pause=(Switch)find(first.getWindow().getDecorView(),"Keep TEMPER paused");if(pause==null||!pause.isChecked())throw new AssertionError("Must default paused");pause.performClick();first.finish();
            });
            waitForIdleSync();Activity second=launch();
            runOnMainSync(()->{
                click(second,"Settings");Switch pause=(Switch)find(second.getWindow().getDecorView(),"Keep TEMPER paused");if(pause==null||pause.isChecked())throw new AssertionError("Pause preference did not persist");
                click(second,"Clear local preferences");Switch reset=(Switch)find(second.getWindow().getDecorView(),"Keep TEMPER paused");if(reset==null||!reset.isChecked())throw new AssertionError("Clear must restore paused");second.finish();
            });
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
            result.putString("stream","PASS: foundation UI/pause/reset; Keystore encrypted account, password rejection, sign-in/out, persistent session and tamper rejection\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable failure){result.putString("stream","FAIL: "+failure.getClass().getSimpleName()+": "+failure.getMessage());finish(Activity.RESULT_CANCELED,result);}
    }
}
