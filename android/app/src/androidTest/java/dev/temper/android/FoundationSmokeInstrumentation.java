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
            result.putString("stream","PASS: onboarding, fictional preview, pause persistence and clear-to-paused\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable failure){result.putString("stream","FAIL: "+failure.getClass().getSimpleName()+": "+failure.getMessage());finish(Activity.RESULT_CANCELED,result);}
    }
}
