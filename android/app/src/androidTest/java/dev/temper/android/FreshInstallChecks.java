package dev.temper.android;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import dev.temper.android.privacy.AnalysisConsent;
import dev.temper.android.privacy.ConsentStore;
import dev.temper.android.privacy.PowerStore;
import dev.temper.android.inference.ModelFiles;

/** Runs only in a new, isolated QA installation; never changes the owner's consent. */
final class FreshInstallChecks {
    static void run(Instrumentation test){
        var context=test.getTargetContext();
        if(!context.getPackageName().equals("dev.temper.android.qa"))throw new AssertionError("Fresh check requires the isolated QA package");
        if(new PowerStore(context).enabled()||new ConsentStore(context).consented()||AnalysisConsent.autoAccepted(context)||ModelFiles.ready(context))throw new AssertionError("Installation is not fresh");
        Activity activity=test.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{test.runOnMainSync(()->{
            require(activity,"Settings").performClick();require(activity,"Set up and permissions").performClick();
            View optIn=require(activity,"I allow automatic on-phone analysis of supported visible chats while TEMPER is ON");
            View allow=require(activity,"Allow automatic analysis");
            if(!(optIn instanceof CheckBox check)||check.isChecked()||allow.isEnabled())throw new AssertionError("Fresh consent is not explicit");
            optIn.performClick();if(!allow.isEnabled())throw new AssertionError("Consent checkbox did not enable action");allow.performClick();
            require(activity,ModelFiles.bundled(context)?"Prepare offline model":"Download model for offline analysis");
            if(new PowerStore(context).enabled()||ModelFiles.ready(context))throw new AssertionError("Missing model bypassed setup");
        });}finally{test.runOnMainSync(activity::finish);}
    }
    private static View require(Activity activity,String label){View view=find(activity.getWindow().getDecorView(),label);if(view==null)throw new AssertionError("Missing fresh-install control: "+label);return view;}
    private static View find(View view,String label){
        if(label.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
        if(view instanceof TextView text&&label.contentEquals(text.getText()))return view;
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),label);if(found!=null)return found;}
        return null;
    }
}
