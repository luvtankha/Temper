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
    private boolean transportOnly;
    private boolean adapterOnly;
    private boolean consumerOnly;
    private boolean overlayOnly;
    private boolean learningOnly;
    private boolean powerOnly;
    private boolean performanceOnly;
    private boolean performanceBaseline;
    private boolean complexOnly;
    private boolean carouselOnly;
    private boolean freshOnly;
    private boolean modelRecoveryOnly;
    private boolean permissionOnly;
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);permissionOnly=arguments!=null&&"true".equals(arguments.getString("permissionOnly"));modelRecoveryOnly=arguments!=null&&"true".equals(arguments.getString("modelRecoveryOnly"));freshOnly=arguments!=null&&"true".equals(arguments.getString("freshOnly"));carouselOnly=arguments!=null&&"true".equals(arguments.getString("carouselOnly"));performanceBaseline=arguments!=null&&"true".equals(arguments.getString("performanceBaseline"));complexOnly=arguments!=null&&"true".equals(arguments.getString("complexOnly"));performanceOnly=arguments!=null&&"true".equals(arguments.getString("performanceOnly"));powerOnly=arguments!=null&&"true".equals(arguments.getString("powerOnly"));learningOnly=arguments!=null&&"true".equals(arguments.getString("learningOnly"));overlayOnly=arguments!=null&&"true".equals(arguments.getString("overlayOnly"));consumerOnly=arguments!=null&&"true".equals(arguments.getString("consumerOnly"));transportOnly=arguments!=null&&"true".equals(arguments.getString("transportOnly"));adapterOnly=arguments!=null&&"true".equals(arguments.getString("adapterOnly"));start();}
    private Activity launch(){return startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    private View find(View root,String label){
        if(root.getContentDescription()!=null&&label.contentEquals(root.getContentDescription()))return root;
        if(root instanceof TextView text&&label.contentEquals(text.getText()))return root;
        if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),label);if(found!=null)return found;}
        return null;
    }
    private void click(Activity activity,String label){View view=find(activity.getWindow().getDecorView(),label);if(view==null||!view.isEnabled())throw new AssertionError("Missing button: "+label);view.performClick();}
    private void openFeedback(Activity activity){click(activity,"Settings");click(activity,"Rate analysis and manage feedback");}
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
            if(permissionOnly){PermissionRecoveryChecks.run(this);result.putString("stream","PASS: isolated real overlay AppOp revocation stops power/service/windows; no host chat read\n");finish(Activity.RESULT_OK,result);return;}
            if(modelRecoveryOnly){ModelRecoveryChecks.run(getTargetContext());result.putString("stream","PASS: isolated corrupt same-size model rejected and discarded, missing-model retry state and restored valid real inference; no host chat read\n");finish(Activity.RESULT_OK,result);return;}
            if(freshOnly){FreshInstallChecks.run(this);result.putString("stream","PASS: isolated fresh install defaults OFF, no consent/model, unchecked explicit opt-in and missing-model setup; no host chat read\n");finish(Activity.RESULT_OK,result);return;}
            if(complexOnly){ComplexContextChecks.run(getContext(),getTargetContext());result.putString("stream","PASS: 28 fictional complex English/Hinglish held-out chats, context-sensitive identical replies, four live repair transitions, first-message spectrum and stopped analysis rejection\n");finish(Activity.RESULT_OK,result);return;}
            if(performanceOnly){PerformanceChecks.run(getTargetContext(),performanceBaseline);result.putString("stream","PASS: generated conversation timing recorded\n");finish(Activity.RESULT_OK,result);return;}
            if(carouselOnly){CarouselUiChecks.run(this);restoreSetup(originalSetup);result.putString("stream","PASS: continuous own-home gestures, exact snap, forward/reverse selection, persistence, 51-item bounded slots, width/font-scale layouts and OFF/ON rendering; no host chat read\n");finish(Activity.RESULT_OK,result);return;}
            if(powerOnly){Activity activity=launch();runOnMainSync(()->{PowerUiChecks.run(activity);activity.finish();});restoreSetup(originalSetup);result.putString("stream","PASS: compact ON/OFF home, consent migration and explicit setup gating, saved power state and one-tap OFF; no host chat read\n");finish(Activity.RESULT_OK,result);return;}
            if(learningOnly){
                if(!BuildConfig.LEARNING_URL.equals("https://feedback.invalid"))throw new AssertionError("Use the isolated test origin for this UI-only check");
                var learningPrefs=getTargetContext().getSharedPreferences("temper_learning",0);var original=new java.util.HashMap<String,Object>(learningPrefs.getAll());if(dev.temper.android.learning.LearningConsent.SESSION.review()!=null)throw new AssertionError("Do not overwrite pending user feedback");
                try{
                    learningPrefs.edit().clear().commit();Activity activity=launch();runOnMainSync(()->{
                        openFeedback(activity);CheckBox agree=(CheckBox)find(activity.getWindow().getDecorView(),"I allow optional rated conversation feedback as described above");View allow=find(activity.getWindow().getDecorView(),"Allow optional feedback");if(agree==null||agree.isChecked()||allow==null||allow.isEnabled())throw new AssertionError("Feedback must require its own unchecked opt-in");
                        click(activity,"Not now");if(new dev.temper.android.learning.LearningConsent(activity).accepted())throw new AssertionError("Decline enabled feedback");openFeedback(activity);agree=(CheckBox)find(activity.getWindow().getDecorView(),"I allow optional rated conversation feedback as described above");agree.performClick();click(activity,"Allow optional feedback");
                        var session=dev.temper.android.learning.LearningConsent.SESSION;session.arm();session.observe(new dev.temper.android.adapters.VisibleConversation(dev.temper.android.adapters.VisibleConversation.Status.AVAILABLE,"a".repeat(64),java.util.List.of(new dev.temper.android.adapters.VisibleConversation.Turn("1".repeat(64),dev.temper.android.adapters.VisibleConversation.Role.LOCAL,"How was the fictional project?"),new dev.temper.android.adapters.VisibleConversation.Turn("2".repeat(64),dev.temper.android.adapters.VisibleConversation.Role.REMOTE,"I am happy with my fictional result."),new dev.temper.android.adapters.VisibleConversation.Turn("3".repeat(64),dev.temper.android.adapters.VisibleConversation.Role.LOCAL,"Congratulations.")),new dev.temper.android.adapters.ScreenObservation.Bounds(0,100,400,150)),new float[]{.1f,.9f,.1f,.1f,.1f,.1f,.1f,.1f});
                        click(activity,"Back");openFeedback(activity);View send=find(activity.getWindow().getDecorView(),"Send rating and reviewed text");if(send==null||send.isEnabled())throw new AssertionError("No default rating allowed");View rating=find(activity.getWindow().getDecorView(),"5 • Very good");if(!(rating instanceof android.widget.RadioButton))throw new AssertionError("Only a quality rating is needed");rating.performClick();if(!send.isEnabled())throw new AssertionError("Rating did not enable reviewed submission");
                        // Never click Send: no fixture is transmitted, and no host app is touched.
                        TextView oldText=(TextView)find(activity.getWindow().getDecorView(),"Other side: I am happy with my fictional result.");if(oldText==null)throw new AssertionError("Generated review text missing");
                        click(activity,"Discard session");if(session.review()!=null)throw new AssertionError("Discard retained text");if(oldText.length()!=0||send.hasOnClickListeners())throw new AssertionError("Discard retained rendered text or its send closure");click(activity,"Stop sharing and discard pending feedback");if(new dev.temper.android.learning.LearningConsent(activity).accepted())throw new AssertionError("Stop sharing did not withdraw consent");activity.finish();
                    });
                }finally{dev.temper.android.learning.LearningConsent.discard();var editor=learningPrefs.edit().clear();for(var item:original.entrySet()){Object value=item.getValue();if(value instanceof Integer number)editor.putInt(item.getKey(),number);else if(value instanceof String string)editor.putString(item.getKey(),string);else if(value instanceof Boolean flag)editor.putBoolean(item.getKey(),flag);}editor.commit();restoreSetup(originalSetup);}
                result.putString("stream","PASS: separate opt-in, decline, generated session review, no default rating, five quality choices, discard and withdrawal; no upload or user emotion labels\n");finish(Activity.RESULT_OK,result);return;
            }
            if(overlayOnly){
                PowerOverlayChecks.run(this,getTargetContext());
                Activity dummy=startActivitySync(new Intent(getTargetContext(),DebugOverlayActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                try{runOnMainSync(()->dev.temper.android.overlay.OverlayInteractionChecks.run(getTargetContext()));}
                finally{runOnMainSync(dummy::finish);}
                restoreSetup(originalSetup);result.putString("stream","PASS: automatic generated-chat model switching, home/notification OFF, floating service and active-analysis drag/tap/cancel/popup/keyboard/restore checks on our dummy screen; no host chat read\n");finish(Activity.RESULT_OK,result);return;
            }
            if(consumerOnly){runOnMainSync(()->{try{ConsumerChecks.run(getContext(),getTargetContext());}catch(Exception failure){throw new RuntimeException(failure);}});ConsumerChecks.modelChecks(getContext(),getTargetContext());PipelineContinuityChecks.run(this,getTargetContext());Activity activity=launch();runOnMainSync(()->{if(find(activity.getWindow().getDecorView(),"Choose your avatar")==null)throw new AssertionError("Carousel home missing");click(activity,"Settings");click(activity,"Set up and permissions");if(find(activity.getWindow().getDecorView(),"Set up TEMPER")==null)throw new AssertionError("On-device setup missing");activity.finish();});restoreSetup(originalSetup);result.putString("stream","PASS: 9 avatars x 8 expressions, paid selection gate, independent tokenizer references, real INT8 phone model, suspended pipeline recovery and stale-result suppression, remote speaker invariance, carousel home and private-analysis setup\n");finish(Activity.RESULT_OK,result);return;}
            if(adapterOnly){WhatsAppAdapterChecks.run(getContext(),getTargetContext());restoreSetup(originalSetup);result.putString("stream","PASS: observed WhatsApp layouts, date separators, clipped rows, roles, privacy and unsupported-content rejection\n");finish(Activity.RESULT_OK,result);return;}
            if(transportOnly){LiveTransportChecks.run(getTargetContext());restoreSetup(originalSetup);result.putString("stream","PASS: generated-text USB request reached actual model backend; stopped/revoked requests blocked; original consent restored\n");finish(Activity.RESULT_OK,result);return;}
            AdapterContractChecks.run();
            RegistryChecks.run();
            ProbeChecks.run(getTargetContext());
            WhatsAppAdapterChecks.run(getContext(),getTargetContext());
            OverlayPlacementChecks.run();
            LiveChecks.run(getTargetContext());
            PrivacyChecks.run(getTargetContext());
            runOnMainSync(()->{try{CharacterChecks.run(getTargetContext());AnalyticsChecks.run(getTargetContext());}catch(Exception failure){throw new RuntimeException(failure);}});
            getTargetContext().getSharedPreferences("MainActivity",0).edit().clear().commit();
            Activity first=launch();
            runOnMainSync(()->{
                if(find(first.getWindow().getDecorView(),"TEMPER")==null)throw new AssertionError("Onboarding missing");
                click(first,"Settings");click(first,"Fictional preview");if(find(first.getWindow().getDecorView(),"Eight compact expressions")==null)throw new AssertionError("Preview missing");
            });
            waitForIdleSync();
            runOnMainSync(()->{
                var c=character(first.getWindow().getDecorView());if(c==null||!c.isAttachedToWindow())throw new AssertionError("Character preview not attached");
                var previewSelection=new dev.temper.android.character.AvatarSelection(getTargetContext(),new dev.temper.android.store.PurchaseStore(getTargetContext())::owned).selected();
                if(c.avatar()!=previewSelection)throw new AssertionError("Fictional preview ignored selected avatar");
                c.setEmotion(dev.temper.android.character.Emotion.HAPPY,true);c.setEmotion(dev.temper.android.character.Emotion.SURPRISED,true);
            });
            android.os.SystemClock.sleep(400);waitForIdleSync();
            runOnMainSync(()->{
                var c=character(first.getWindow().getDecorView());if(c.transitioning()||c.emotion()!=dev.temper.android.character.Emotion.SURPRISED)throw new AssertionError("Interrupted transition did not settle");
                c.setEmotion(dev.temper.android.character.Emotion.SAD,true);click(first,"Back");if(c.transitioning())throw new AssertionError("Detached character kept animating");
                PowerUiChecks.run(first);first.finish();
            });
            waitForIdleSync();Activity second=launch();
            runOnMainSync(()->{
                if(find(second.getWindow().getDecorView(),"ON")==null||new dev.temper.android.privacy.PowerStore(second).enabled())throw new AssertionError("OFF state did not persist");
                click(second,"Settings");click(second,"Developer tools");click(second,"Accessibility and consent");
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
            result.putString("stream","PASS: compact overlay grounding/density/keyboard/failure geometry; observed WhatsApp layout fixture/roles/clipping/dedup/uncertainty/privacy; one-shot probe/composer gate/text-free export/pause cleanup; adapter contract bounds/roles/immutability/failure/redaction; one-button home/setup/consent migration and persisted OFF; consent UI opt-in/resume/revoke and package/version gates; Keystore encrypted account, password rejection, sign-in/out, persistent session and tamper rejection; original setup preferences restored\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable failure){restoreSetup(originalSetup);result.putString("stream","FAIL: "+failure.getClass().getSimpleName()+": "+failure.getMessage());finish(Activity.RESULT_CANCELED,result);}
    }
}
