package dev.temper.android;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import android.text.InputType;
import dev.temper.android.auth.DeviceAccountStore;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import android.content.Intent;
import android.provider.Settings;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityManager;
import dev.temper.android.privacy.ConsentStore;
import dev.temper.android.accessibility.TemperAccessibilityService;
import dev.temper.android.accessibility.ProbeState;
import dev.temper.android.adapters.ScreenObservation;
import dev.temper.android.accessibility.LayoutMetadata;
import dev.temper.android.accessibility.ParseProbeState;
import dev.temper.android.character.CharacterView;
import dev.temper.android.character.Emotion;
import dev.temper.android.character.Avatar;
import dev.temper.android.character.AvatarSelection;
import dev.temper.android.store.PlayStore;
import dev.temper.android.store.PurchaseStore;
import dev.temper.android.overlay.FloatingOverlayService;
import dev.temper.android.inference.ModelFiles;
import dev.temper.android.privacy.AnalysisConsent;
import dev.temper.android.privacy.PowerStore;

/** Native onboarding, device account and explicit accessibility consent controls. */
public final class MainActivity extends androidx.activity.ComponentActivity {
    private LinearLayout content;
    private dev.temper.android.home.AvatarHome avatarHome;
    private final ExecutorService accountWorker=Executors.newSingleThreadExecutor();
    private final android.os.Handler feedbackUi=new android.os.Handler(android.os.Looper.getMainLooper());
    private final android.os.Handler powerUi=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable powerStartPoll=this::pollPowerStart;
    private final Runnable powerStatusRefresh=()->{if(!isDestroyed()&&this.activityResumed&&this.homeScreen)home();};
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener powerPreferenceListener=(preferences,key)->{if(("powerEnabled".equals(key)||"paused".equals(key)||"powerStopSequence".equals(key))&&this.activityResumed&&this.homeScreen&&!isDestroyed()){powerUi.removeCallbacks(powerStatusRefresh);powerUi.post(powerStatusRefresh);}};
    private android.content.SharedPreferences powerPreferences;
    private boolean powerStartPending;
    private boolean powerStartAwaitingService;
    private long powerStartDeadline;
    private long powerStartStopSequence;
    private boolean activityResumed;
    private final Runnable feedbackStateListener=()->feedbackUi.post(()->{if(!isDestroyed()&&activityResumed&&this.feedbackScreen)analysisFeedback();});
    private DeviceAccountStore accountStore;
    private long pageVersion;
    private boolean consentScreen;
    private boolean liveScreen;
    private boolean shopScreen,analysisScreen,feedbackScreen,downloading;
    private PlayStore shop;
    @Override public void onResume(){super.onResume();activityResumed=true;if(!powerStartPending&&new PowerStore(this).enabled()&&(!setupReady()||!FloatingOverlayService.running()))turnOff();FloatingOverlayService.appVisible(true);FloatingOverlayService.checkoutVisible(false);if(shopScreen){shop.restore();avatars();}else if(analysisScreen)privateAnalysis();else if(feedbackScreen)analysisFeedback();else if(consentScreen)consent();else if(liveScreen)liveAnalysis();else if(homeScreen)home();}
    @Override public void onPause(){activityResumed=false;powerUi.removeCallbacks(powerStatusRefresh);feedbackUi.removeCallbacksAndMessages(null);if(feedbackScreen&&content!=null)clearFeedbackViews(content);FloatingOverlayService.appVisible(false);super.onPause();}
    @Override public void onCreate(Bundle state){super.onCreate(state);getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){@Override public void handleOnBackPressed(){if(homeScreen)finish();else home();}});if(android.os.Build.VERSION.SDK_INT>=31)getWindow().setHideOverlayWindows(true);accountStore=new DeviceAccountStore(this);shop=new PlayStore(this,()->{FloatingOverlayService.changed();if(shopScreen)avatars();});powerPreferences=new ConsentStore(this).preferences();powerPreferences.registerOnSharedPreferenceChangeListener(powerPreferenceListener);dev.temper.android.learning.LearningOperations.SHARED.listen(feedbackStateListener);home();}
    @Override public void onDestroy(){activityResumed=false;if(powerPreferences!=null)powerPreferences.unregisterOnSharedPreferenceChangeListener(powerPreferenceListener);cancelPendingPowerOn();powerUi.removeCallbacksAndMessages(null);dev.temper.android.learning.LearningOperations.SHARED.removeListener(feedbackStateListener);feedbackUi.removeCallbacksAndMessages(null);if(content!=null){if(feedbackScreen)clearFeedbackViews(content);content.removeAllViews();}accountWorker.shutdownNow();shop.close();super.onDestroy();}
    private boolean homeScreen;
    private void page(String title){
        avatarHome=null;
        feedbackUi.removeCallbacksAndMessages(null);if(feedbackScreen&&content!=null)clearFeedbackViews(content);pageVersion++;homeScreen=false;consentScreen=false;liveScreen=false;shopScreen=false;analysisScreen=false;feedbackScreen=false;
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);int pad=(int)(24*getResources().getDisplayMetrics().density);content.setPadding(pad,pad*2,pad,pad);content.setBackgroundColor(Color.rgb(21,16,25));scroll.addView(content);showInsetContent(scroll);text(title,28);
    }
    private void text(String value,int size){TextView text=new TextView(this);text.setText(value);text.setTextColor(Color.rgb(242,230,248));text.setTextSize(size);text.setPadding(0,12,0,12);content.addView(text);}
    private void button(String label,Runnable action){Button button=new Button(this);button.setText(label);button.setOnClickListener(v->action.run());content.addView(button);}
    private void home(){
        if(homeScreen&&avatarHome!=null){avatarHome.bind(homePowerActive(),powerStartPending);return;}
        page("TEMPER");homeScreen=true;
        PurchaseStore ownership=new PurchaseStore(this);AvatarSelection selection=new AvatarSelection(this,ownership::owned);Avatar selected=selection.selected();java.util.List<Avatar> catalog=Avatar.homeCatalog(ownership::owned);
        if(!catalog.contains(selected)){selected=Avatar.ASTRA;selection.select(selected);}
        avatarHome=new dev.temper.android.home.AvatarHome(this,catalog,selected,avatar->{selection.select(avatar);FloatingOverlayService.changed();},wasOn->{if(wasOn){turnOff();home();}else turnOn();},this::settings);
        showInsetContent(avatarHome);avatarHome.bind(homePowerActive(),powerStartPending);
    }
    private void showInsetContent(android.view.View view){
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view,(v,insets)->{androidx.core.graphics.Insets safe=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout()|androidx.core.view.WindowInsetsCompat.Type.ime());v.setPadding(safe.left,safe.top,safe.right,safe.bottom);return insets;});
        setContentView(view);androidx.core.view.ViewCompat.requestApplyInsets(view);
    }
    private boolean homePowerActive(){return !powerStartPending&&new PowerStore(this).enabled()&&setupReady()&&FloatingOverlayService.running();}
    private boolean setupReady(){return AnalysisConsent.autoAccepted(this)&&ModelFiles.ready(this)&&Settings.canDrawOverlays(this)&&serviceEnabled()&&TemperAccessibilityService.connected();}
    private void turnOn(){
        if(powerStartPending)return;
        if(new PowerStore(this).enabled()){if(homePowerActive()){home();return;}turnOff();}
        if(!setupReady()){privateAnalysis();return;}
        if(FloatingOverlayService.running()){
            powerStartPending=true;powerStartAwaitingService=false;powerStartDeadline=android.os.SystemClock.elapsedRealtime()+5000;
            FloatingOverlayService.stop(this);powerStartStopSequence=new PowerStore(this).stopSequence();home();powerUi.post(powerStartPoll);return;
        }
        activatePower();
    }
    private void pollPowerStart(){
        if(!powerStartPending)return;
        if(new PowerStore(this).stopSequence()!=powerStartStopSequence){cancelPendingPowerOn();if(!isDestroyed()&&!isFinishing())home();return;}
        if(isDestroyed()||isFinishing()){cancelPendingPowerOn();return;}
        if(android.os.SystemClock.elapsedRealtime()>=powerStartDeadline){powerStartFailed(powerStartAwaitingService?"The overlay did not start. TEMPER is OFF; check setup and retry.":"The previous overlay did not stop. TEMPER is OFF; retry from setup.");return;}
        if(!setupReady()){powerStartFailed("Setup changed while waiting. TEMPER is OFF; check setup and retry.");return;}
        if(powerStartAwaitingService){
            if(!new PowerStore(this).enabled()){powerStartFailed("Could not turn ON. TEMPER is OFF; check setup and retry.");return;}
            if(!FloatingOverlayService.running()){powerUi.postDelayed(powerStartPoll,100);return;}
            powerStartPending=powerStartAwaitingService=false;powerStartDeadline=0;powerStartStopSequence=0;Toast.makeText(this,"TEMPER is ON. Open a supported chat.",Toast.LENGTH_LONG).show();home();return;
        }
        if(new PowerStore(this).enabled()){powerStartPending=powerStartAwaitingService=false;powerStartDeadline=0;powerStartStopSequence=0;home();return;}
        if(FloatingOverlayService.running()){powerUi.postDelayed(powerStartPoll,100);return;}
        powerStartPending=false;powerStartDeadline=0;powerStartStopSequence=0;activatePower();
    }
    private void activatePower(){
        if(isDestroyed()||isFinishing())return;
        if(new PowerStore(this).enabled()){home();return;}
        if(!setupReady()){powerStartFailed("TEMPER is OFF. Check setup before turning ON.");return;}
        try{powerStartPending=powerStartAwaitingService=true;powerStartDeadline=android.os.SystemClock.elapsedRealtime()+5000;powerStartStopSequence=new PowerStore(this).stopSequence();new PowerStore(this).setEnabled(true);FloatingOverlayService.start(this);home();powerUi.post(powerStartPoll);}
        catch(RuntimeException unavailable){powerStartFailed("Could not turn ON. TEMPER is OFF; check setup and retry.");}
    }
    private void powerStartFailed(String message){turnOff();if(!isDestroyed()&&!isFinishing()){Toast.makeText(this,message,Toast.LENGTH_LONG).show();privateAnalysis();}}
    private void cancelPendingPowerOn(){boolean pending=powerStartPending;powerStartPending=powerStartAwaitingService=false;powerStartDeadline=0;powerStartStopSequence=0;powerUi.removeCallbacks(powerStartPoll);if(pending)new PowerStore(this).setEnabled(false);}
    private void turnOff(){cancelPendingPowerOn();new PowerStore(this).requestOff();FloatingOverlayService.stop(this);}
    private void character(Avatar avatar,Emotion emotion){CharacterView view=new CharacterView(this);view.setAvatar(avatar);view.setEmotion(emotion,false);float density=getResources().getDisplayMetrics().density;var size=new LinearLayout.LayoutParams(Math.round(64*density),Math.round(88*density));size.gravity=android.view.Gravity.CENTER_HORIZONTAL;content.addView(view,size);}
    private void avatars(){
        page("Choose your companion");shopScreen=true;text("One-time avatar upgrades",20);text("Every companion uses the same analysis. A purchase changes appearance and includes all eight expressions. Restore purchases with the same Google Play account.",16);text(shop.status(),14);
        Avatar selected=new AvatarSelection(this,shop::owned).selected();
        for(Avatar avatar:Avatar.values()){
            text(avatar.displayName()+(selected==avatar?" • Selected":""),22);character(avatar,Emotion.HAPPY);text(avatar.description(),15);
            button("Preview "+avatar.displayName(),()->avatarPreview(avatar));
            if(shop.owned(avatar))button(selected==avatar?"Selected companion":"Use "+avatar.displayName(),()->{new AvatarSelection(this,shop::owned).select(avatar);FloatingOverlayService.changed();avatars();});
            else{String price=shop.price(avatar);Button buy=new Button(this);buy.setText(price==null?"Available at store launch":"Buy "+avatar.displayName()+" • "+price);buy.setEnabled(shop.canBuy(avatar));buy.setOnClickListener(v->shop.buy(avatar));content.addView(buy);}
        }
        button("Restore purchases",shop::restore);button("Back",this::home);
    }
    private void avatarPreview(Avatar avatar){page(avatar.displayName()+" preview");text("All expressions are included with this companion",16);for(Emotion emotion:Emotion.values()){character(avatar,emotion);text(emotion.label(),16);}button("Back to companions",this::avatars);}
    private void privateAnalysis(){
        page("Set up TEMPER");analysisScreen=true;ConsentStore base=new ConsentStore(this);
        boolean accepted=AnalysisConsent.autoAccepted(this),modelReady=ModelFiles.ready(this),overlayAllowed=Settings.canDrawOverlays(this),enabled=serviceEnabled(),connected=TemperAccessibilityService.connected();
        text("One-time setup",21);
        if(!accepted){
            text("When you turn TEMPER ON, it automatically analyzes the currently visible supported one-to-one WhatsApp chat, including each supported chat you switch to. Android Accessibility reads up to eight fully visible plain-text messages, sent/received roles, timestamps, a local conversation identifier and the composer position. It does not read drafts, contact lists or unseen chat history, tap chat controls or send messages.",16);
            text("Normal analysis stays in bounded memory on this phone. It is never uploaded, saved or logged. Changing chats clears the previous analysis before starting the new chat; leaving a supported chat clears its visible text. Keyboard changes do not turn TEMPER OFF. Use OFF or revoke access to stop. Closing the running app stops automatic analysis; turn ON again after reopening. Android Accessibility settings can disable TEMPER at any time.",16);
            text("Optional ratings and reviewed conversation feedback require separate permission and an explicit Send rating action. They are not needed to use TEMPER. Estimates can be wrong and do not establish anyone's feelings.",15);
            CheckBox agree=new CheckBox(this);agree.setText("I allow automatic on-phone analysis of supported visible chats while TEMPER is ON");agree.setTextColor(Color.WHITE);content.addView(agree);
            Button save=new Button(this);save.setText("Allow automatic analysis");save.setEnabled(false);content.addView(save);agree.setOnCheckedChangeListener((v,checked)->save.setEnabled(checked));save.setOnClickListener(v->{turnOff();AnalysisConsent.acceptAuto(this);privateAnalysis();});
        }else if(!modelReady){
            boolean bundled=ModelFiles.bundled(this);
            text((bundled?"Prepare the bundled offline model • ":"Download the offline model • ")+((ModelFiles.SIZE+999_999)/1_000_000)+" MB",17);
            text(bundled?"The verified public model is included in this app. Prepare it once without internet; no messages are accessed. Allow about 150 MB of free space and keep this screen open.":"Download public model weights from Hugging Face once. The download shares your network address with the host and sends no messages. Allow about 150 MB of free space and keep this screen open.",14);
            Button download=new Button(this);download.setText(downloading?"Preparing model…":bundled?"Prepare offline model":"Download model for offline analysis");download.setEnabled(!downloading);content.addView(download);
            TextView progress=new TextView(this);progress.setTextColor(Color.WHITE);content.addView(progress);long version=pageVersion;
            download.setOnClickListener(v->{downloading=true;download.setEnabled(false);accountWorker.execute(()->{String failure=null;try{ModelFiles.download(this,percent->runOnUiThread(()->{if(!isDestroyed()&&version==pageVersion)progress.setText("Preparing model: "+percent+"%" );}),this::isDestroyed);}catch(Exception error){failure=error.getMessage();}String message=failure;runOnUiThread(()->{downloading=false;if(isDestroyed())return;if(message!=null)Toast.makeText(this,message,Toast.LENGTH_LONG).show();if(analysisScreen)privateAnalysis();});});});
        }else if(!overlayAllowed){
            text("Allow the small movable companion to appear over chat apps. Only the character and its compact analytics panel receive touches.",16);
            button("Allow display over other apps",()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:"+getPackageName()))));
        }else if(!enabled||!connected){
            text(enabled?"Reconnect the Android Accessibility service":"Enable TEMPER in Android Accessibility settings",18);
            text(enabled?"Android shows Use TEMPER enabled, but the service is not connected. Switch Use TEMPER OFF and ON once, then return here.":"Open Downloaded/Installed apps in Accessibility settings, choose TEMPER, turn Use TEMPER ON and confirm Android's disclosure. Return here afterward.",16);
            button("Open Android Accessibility settings",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        }else{
            text("Ready to turn ON",21);text("Turn ON, then use supported chats normally. There is no chat-selection timer. Drag the companion to either side; tap it for the emotional spectrum.",16);
            boolean wasEnabled=new PowerStore(this).enabled();Button power=new Button(this);power.setText(powerStartPending?"Turning ON…":wasEnabled?"OFF":"ON");power.setEnabled(!powerStartPending);power.setOnClickListener(v->{if(wasEnabled){turnOff();privateAnalysis();}else turnOn();});content.addView(power);
        }
        text("Analysis permission: "+(accepted?"Allowed":"Needed")+"\nOffline model: "+(modelReady?"Ready":"Needed")+"\nOverlay permission: "+(overlayAllowed?"Allowed":"Needed")+"\nAccessibility: "+(enabled?(connected?"Connected":"Reconnect needed"):"Enable needed"),14);
        if(accepted){button("Revoke analysis access",()->{turnOff();base.revoke();privateAnalysis();});}
        if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)button("Allow notification stop control",()->requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},41));
        text("Automatic analysis supports verified WhatsApp 2.26.37.73 in portrait. One complete incoming message can show emotion bars; direction needs two incoming turns and earlier replies from you. Other apps can show the companion without estimates. English works best; Hinglish, sarcasm and direction can be unreliable. Scores describe language, not a person's internal feelings.",14);button("Refresh status",this::privateAnalysis);button("Back",this::home);
    }
    private void settings(){
        page("Settings");text(new PowerStore(this).enabled()?"TEMPER is ON":"TEMPER is OFF",18);button("Set up and permissions",this::privateAnalysis);
        text("Normal analysis saves no chat text and works offline after one-time model preparation. Optional rated conversation feedback can send reviewed text and estimates to the separate feedback service. Paid avatars use Google Play; only purchase verification data goes to the store server.",16);
        button("Rate analysis and manage feedback",this::analysisFeedback);
        button("Turn TEMPER OFF",()->{turnOff();settings();});
        button("Choose companion",this::avatars);
        button("Fictional preview",this::preview);
        button("Privacy and model information",()->{page("Privacy and model information");text("Normal chat analysis stays in memory on this phone. It is never saved or uploaded. Optional quality feedback requires separate permission and a Send rating action to share selected reviewed conversation segments and estimates with Luv Tankha's feedback service. No background uploads occur. Feedback can be retained encrypted for up to 90 days and used privately to evaluate and improve analysis. Submitted records can be deleted in the feedback page; a previously trained model cannot be promised to forget its influence. Device uninstall does not delete submitted feedback or Google purchases.",16);text("Google Play handles payment information. TEMPER sends signed purchase receipts to its HTTPS verification server and Google to validate purchases and restore ownership. A short-lived, signed ownership claim is cached locally. No chat data enters the store server. Verified public model weights are bundled in this app and prepared offline. A legacy build missing that asset can download public weights; that model host receives ordinary connection metadata including your network address, never chat text.",16);text("This build uses a SHA-256 verified RoBERTa INT8 model based on English GoEmotions. The original model is distributed by SamLowe under the MIT license. Model updates require publisher evaluation and a new app release. Hinglish, sarcasm and conversation direction are not validated for accuracy. Scores estimate language and cannot establish anyone's feelings. Support: luvtankha06@gmail.com.",16);button("Back",this::settings);});
        button("Remove offline model and turn OFF",()->{turnOff();accountWorker.execute(()->{try{java.nio.file.Files.deleteIfExists(ModelFiles.file(this).toPath());}catch(Exception ignored){}runOnUiThread(()->{if(!isDestroyed())settings();});});});
        button("Clear inspection data and turn OFF",()->{turnOff();dev.temper.android.privacy.PrivacyControls.clearInspection(this);settings();});
        if(BuildConfig.DEBUG)button("Remove USB connection and pause",()->{try{dev.temper.android.privacy.PrivacyControls.removeConnection(this);settings();}catch(IllegalStateException failure){Toast.makeText(this,failure.getMessage(),Toast.LENGTH_LONG).show();}});
        if(BuildConfig.DEBUG)button("Developer tools",this::developerTools);button("Back",this::home);
    }
    private void developerTools(){page("Developer tools");button("Accessibility and consent",this::consent);button("Live analysis (USB demo)",this::liveAnalysis);button("Device account",this::account);button("Back",this::settings);}
    private void analysisFeedback(){
        page("Rate analysis");feedbackScreen=true;
        var consent=new dev.temper.android.learning.LearningConsent(this);
        var operation=dev.temper.android.learning.LearningOperations.SHARED.state();
        text("Optional quality feedback",21);
        text("TEMPER produces the analysis. You can rate its quality from 1 to 5; no emotion labels or explanation are required.",16);
        text("If you enable a feedback session, TEMPER keeps up to 64 visible turns and 32 estimates from one selected supported conversation in memory for up to ten minutes. It cannot read unseen history. After review, Send rating transmits the shown text, sent/received roles, estimates, rating, app/model version and a random deletion identifier over HTTPS to Luv Tankha's feedback service. Accepted feedback is encrypted and kept for up to 90 days to evaluate and improve analysis quality. It is not sold or published. Core analysis does not require sharing.",16);
        text("Automatic redaction removes common phone numbers, email addresses, URLs and handles, but can miss names or other details. Share only if you are 18+ and everyone whose messages are included has agreed. Review what will be sent; discard it if identifying or sensitive details remain. Sent feedback may influence later updates; deleting records cannot guarantee removal of influence from an already trained model.",15);
        if(!dev.temper.android.learning.LearningConsent.configured())text("Feedback sharing is unavailable until the publisher deploys the service. No conversation is being recorded for feedback.",15);
        if(operation.busy())text("A feedback request is in progress. New sharing and deletion requests wait until it finishes. You can still stop sharing or discard pending text.",15);
        else if(operation.message()!=null)text(operation.message(),15);
        if(!consent.accepted()){
            CheckBox agree=new CheckBox(this);agree.setText("I allow optional rated conversation feedback as described above");agree.setTextColor(Color.WHITE);content.addView(agree);
            Button allow=new Button(this);allow.setText("Allow optional feedback");allow.setEnabled(false);content.addView(allow);agree.setOnCheckedChangeListener((v,checked)->allow.setEnabled(checked&&consent.canAccept()));allow.setOnClickListener(v->{try{consent.accept();analysisFeedback();}catch(IllegalStateException unavailable){Toast.makeText(this,"Feedback permission could not be saved. Delete prior feedback if the service changed.",Toast.LENGTH_LONG).show();}});button("Not now",()->{consent.decline();home();});
            if(!operation.busy()&&dev.temper.android.learning.LearningConsent.configured()&&!consent.canAccept())text("Delete previously submitted feedback before using this new service.",14);
        }else{
            var review=dev.temper.android.learning.LearningConsent.SESSION.review();
            if(review!=null&&!review.analyses().isEmpty()){
                dev.temper.android.learning.LearningConsent.SESSION.finish();text("Review: "+review.turns().size()+" visible turns, "+review.analyses().size()+" estimates",18);
                expireFeedbackReview(review.id(),pageVersion);
                for(var turn:review.turns())text((turn.role().equals("REMOTE")?"Other side: ":"You: ")+turn.text(),14);
                for(var estimate:review.analyses()){text("Estimate for other-side turn "+(estimate.target()+1)+": "+estimate.currentState()+" • "+estimate.direction(),14);StringBuilder bars=new StringBuilder();float[] scores=estimate.scores();for(int i=0;i<8;i++){if(i>0)bars.append(" • ");bars.append(Emotion.values()[i].label()).append(' ').append(Math.round(scores[i]*100)).append('%');}text(bars.toString(),13);}
                text("How good was the analysis?",20);RadioGroup rating=new RadioGroup(this);String[] names={"1 • Very poor","2 • Poor","3 • Okay","4 • Good","5 • Very good"};for(int i=0;i<5;i++){RadioButton choice=new RadioButton(this);choice.setId(android.view.View.generateViewId());choice.setTag(i+1);choice.setText(names[i]);choice.setTextColor(Color.WHITE);rating.addView(choice);}content.addView(rating);
                Button send=new Button(this);send.setText("Send rating and reviewed text");send.setEnabled(false);content.addView(send);rating.setOnCheckedChangeListener((v,id)->send.setEnabled(!dev.temper.android.learning.LearningOperations.SHARED.state().busy()&&id!=-1&&dev.temper.android.learning.LearningConsent.SESSION.remainingMillis(review.id())>0));
                send.setOnClickListener(v->{if(dev.temper.android.learning.LearningOperations.SHARED.state().busy())return;RadioButton selected=rating.findViewById(rating.getCheckedRadioButtonId());if(selected==null)return;int value=(Integer)selected.getTag();send.setEnabled(false);String body;try{body=feedbackBody(review,value);}catch(Exception invalid){Toast.makeText(this,"Session expired or could not be prepared",Toast.LENGTH_LONG).show();analysisFeedback();return;}String sessionId=review.id();android.content.Context application=getApplicationContext();dev.temper.android.learning.LearningOperations.SHARED.start(()->{try{new dev.temper.android.learning.LearningClient().submit(application,body);dev.temper.android.learning.LearningConsent.discard(sessionId);return "Thank you. Your rating and reviewed session were saved.";}catch(Exception failure){return "Rating was not confirmed. Retry manually or discard; the session expires after ten minutes.";}});});
                button("Discard session",()->{dev.temper.android.learning.LearningConsent.discard();analysisFeedback();});
            }else{
                text("Start a session, use the selected conversation, then return here to rate the analysis. Leaving the conversation stops recording; the pending review expires after ten minutes.",15);
                CheckBox permission=new CheckBox(this);permission.setText("I am 18+ and everyone in my next selected chat agrees to sharing its visible messages with my rating");permission.setTextColor(Color.WHITE);content.addView(permission);
                Button start=new Button(this);start.setText("Start a conversation to rate");start.setEnabled(false);content.addView(start);boolean ready=new PowerStore(this).enabled()&&AnalysisConsent.auto(this)&&setupReady();permission.setOnCheckedChangeListener((v,checked)->start.setEnabled(checked&&ready&&!dev.temper.android.learning.LearningOperations.SHARED.state().busy()));
                if(!ready){text("Turn TEMPER ON from the home screen before starting a conversation to rate.",15);button("Set up TEMPER first",this::privateAnalysis);}
                start.setOnClickListener(v->{if(!new PowerStore(this).enabled()||!AnalysisConsent.auto(this)||!setupReady()||!permission.isChecked()||dev.temper.android.learning.LearningOperations.SHARED.state().busy()){analysisFeedback();return;}try{if(dev.temper.android.accessibility.LiveCaptureState.arm(this)){consent.arm();Intent launch=getPackageManager().getLaunchIntentForPackage(ConsentStore.WHATSAPP);if(launch!=null)startActivity(launch);else Toast.makeText(this,"Open a supported WhatsApp chat within one minute",Toast.LENGTH_LONG).show();}}catch(IllegalStateException unavailable){Toast.makeText(this,"Feedback session could not start. Check permission and retry.",Toast.LENGTH_LONG).show();analysisFeedback();}});
            }
            button("Stop sharing and discard pending feedback",()->{consent.revoke();analysisFeedback();});
        }
        Button delete=new Button(this);delete.setText("Delete my submitted feedback");delete.setEnabled(!operation.busy());content.addView(delete);delete.setOnClickListener(v->{if(dev.temper.android.learning.LearningOperations.SHARED.state().busy())return;consent.revoke();android.content.Context application=getApplicationContext();dev.temper.android.learning.LearningOperations.SHARED.start(()->{try{new dev.temper.android.learning.LearningClient().delete(application);return "Submitted feedback deleted. Future sharing is off.";}catch(Exception failure){return "Deletion not confirmed. Sharing is off; retry deletion when connected. Do not uninstall before deletion succeeds.";}});});
        text("A request already sent can finish after stopping. Delete submitted feedback to remove it from the active collection. Uninstall removes the local deletion key, so delete before uninstalling. Support: luvtankha06@gmail.com.",14);button("Back",this::home);
    }
    private void expireFeedbackReview(String sessionId,long version){
        long remaining=dev.temper.android.learning.LearningConsent.SESSION.remainingMillis(sessionId);
        feedbackUi.postDelayed(()->{if(isDestroyed()||!activityResumed||!feedbackScreen||pageVersion!=version)return;if(dev.temper.android.learning.LearningConsent.SESSION.remainingMillis(sessionId)>0)expireFeedbackReview(sessionId,version);else analysisFeedback();},Math.max(1,remaining));
    }
    private void clearFeedbackViews(android.view.View view){
        view.setOnClickListener(null);
        if(view instanceof RadioGroup group)group.setOnCheckedChangeListener(null);
        if(view instanceof CompoundButton choice)choice.setOnCheckedChangeListener(null);
        if(view instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++)clearFeedbackViews(group.getChildAt(i));
        if(view instanceof TextView text)text.setText("");
    }
    private String feedbackBody(dev.temper.android.learning.LearningSession.Review review,int rating)throws Exception{
        var current=dev.temper.android.learning.LearningConsent.SESSION.review();if(current==null||!review.id().equals(current.id()))throw new IllegalStateException("Session expired");
        org.json.JSONArray turns=new org.json.JSONArray(),analyses=new org.json.JSONArray();for(var turn:review.turns())turns.put(new org.json.JSONObject().put("index",turn.index()).put("role",turn.role()).put("text",turn.text()));for(var estimate:review.analyses())analyses.put(new org.json.JSONObject().put("target",estimate.target()).put("context",new org.json.JSONArray(estimate.context())).put("scores",new org.json.JSONArray(estimate.scores())).put("currentState",estimate.currentState()).put("direction",estimate.direction()).put("trajectory",estimate.trajectory()));
        return new org.json.JSONObject().put("consentVersion",1).put("adultConfirmed",true).put("participantPermissionConfirmed",true).put("textReviewed",true).put("sessionId",review.id()).put("language","OTHER").put("appVersion",BuildConfig.VERSION_NAME).put("modelHash",ModelFiles.HASH).put("qualityRating",rating).put("turns",turns).put("analyses",analyses).toString();
    }
    private boolean serviceEnabled(){
        AccessibilityManager manager=(AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);
        for(AccessibilityServiceInfo info:manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)){
            android.content.pm.ServiceInfo service=info.getResolveInfo().serviceInfo;
            if(getPackageName().equals(service.packageName)&&TemperAccessibilityService.class.getName().equals(service.name))return true;
        }return false;
    }
    private void consent(){
        page("Accessibility and consent");consentScreen=true;ConsentStore store=new ConsentStore(this);
        text("Android Accessibility can expose screen content. TEMPER will use only recent visible messages and composer bounds in supported WhatsApp chats to estimate language and direction. It will never click controls or send messages. Estimates cannot establish someone's feelings.",16);
        text("Test inspection is limited to a fictional one-to-one chat you select. The layout probe reads identifiers and bounds without text. The separately armed parser reads up to eight visible messages (1000 characters each), their timestamps and a conversation identifier, keeping text only in memory. No composer draft, contact list or unrelated content is read; no raw chats are logged or exported. Pause clears the window. USB processing requires the separate opt-in on the Live analysis page.",16);
        text("You control access: pause stops detection; revoke removes consent and disables the service. Android's Accessibility settings can also disable TEMPER at any time.",16);
        text("System service: "+(serviceEnabled()?"Enabled":"Disabled"),18);
        if(serviceEnabled()&&!TemperAccessibilityService.connected())text("TEMPER service is not connected yet. If this persists after installation or tests, turn Use TEMPER off and on in Android Accessibility settings.",16);
        text(store.paused()?"TEMPER is paused":"TEMPER is resumed",18);
        text("WhatsApp detection: "+(store.consented()&&!store.paused()&&serviceEnabled()&&TemperAccessibilityService.recentlyDetected()?"recent supported event":"no recent supported event"),16);
        if(!store.consented()){
            CheckBox agree=new CheckBox(this);agree.setText("I understand and opt in to selected WhatsApp test-chat inspection");agree.setTextColor(Color.WHITE);content.addView(agree);
            Button accept=new Button(this);accept.setText("Save consent");accept.setEnabled(false);agree.setOnCheckedChangeListener((view,checked)->accept.setEnabled(checked));accept.setOnClickListener(view->{store.accept();consent();});content.addView(accept);
        }else{
            button("Open Android Accessibility settings",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
            button(store.paused()?"Resume TEMPER":"Pause TEMPER",()->{store.pause(!store.paused());consent();});
            button("Revoke consent and disable",()->{store.revoke();consent();});
            if(!store.paused()&&serviceEnabled()){
                button("Arm next fictional chat layout",()->{ParseProbeState.clear(this);LayoutMetadata.clear(this);ProbeState.arm();consent();});
                button("Parse next fictional chat once",()->{ProbeState.clear();LayoutMetadata.clear(this);ParseProbeState.arm(this);consent();});
            }
        }
        text("Test parser: "+ParseProbeState.status(),16);
        text("Character: "+dev.temper.android.overlay.OverlayManager.status(),16);
        text("Test probe: "+ProbeState.status(),16);
        ScreenObservation observed=ProbeState.observation();
        if(observed!=null){
            button("Export text-free layout metadata",()->{try{LayoutMetadata.export(this,observed);Toast.makeText(this,"Text-free metadata saved privately for adapter calibration",Toast.LENGTH_LONG).show();}catch(Exception unavailable){Toast.makeText(this,"Metadata export unavailable",Toast.LENGTH_LONG).show();}});
            button("Show text-free layout metadata",()->{
                page("Text-free layout metadata");
                text("Resource IDs and bounds only. No text or descriptions were read.",16);
                int index=0;for(ScreenObservation.Node node:observed.nodes()){
                    text(index++ +" parent="+node.parentIndex()+" id="+node.resourceId()+" class="+node.className()+" bounds="+node.bounds()+" editable="+node.editable(),12);
                }button("Back",this::consent);
            });
        }
        button("Refresh status",this::consent);button("Back",this::home);
    }
    private void preview(){
        page("Fictional preview");text("Eight compact expressions",22);text("Fictional states for appearance testing. Select an expression to see the transition.",16);
        Avatar selected=new AvatarSelection(this,shop::owned).selected();
        float density=getResources().getDisplayMetrics().density;CharacterView live=new CharacterView(this);live.setAvatar(selected);
        LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(Math.round(64*density),Math.round(88*density));size.gravity=android.view.Gravity.CENTER_HORIZONTAL;content.addView(live,size);
        Spinner chooser=new Spinner(this);String[] labels=java.util.Arrays.stream(Emotion.values()).map(Emotion::label).toArray(String[]::new);
        chooser.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        chooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,android.view.View view,int position,long id){live.setEmotion(Emotion.values()[position],true);}public void onNothingSelected(AdapterView<?> parent){}});content.addView(chooser);
        button("Preview analytics box",()->{
            dev.temper.android.analytics.AnalyticsPanel panel=new dev.temper.android.analytics.AnalyticsPanel(this);
            panel.bind(new dev.temper.android.analytics.OverlaySummary("Fictional concern","Fictional tension rising",new float[]{.1f,.05f,.65f,.1f,.1f,.25f,.05f,.05f},true));
            PopupWindow popup=new PopupWindow(panel,Math.round(280*density),-2,true);popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));popup.setOutsideTouchable(true);popup.setElevation(8*density);popup.showAtLocation(content,android.view.Gravity.CENTER,0,0);
        });
        for(int row=0;row<4;row++){
            LinearLayout pair=new LinearLayout(this);pair.setGravity(android.view.Gravity.CENTER);content.addView(pair);
            for(int col=0;col<2;col++){
                Emotion emotion=Emotion.values()[row*2+col];LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(android.view.Gravity.CENTER);pair.addView(cell,new LinearLayout.LayoutParams(0,-2,1));
                CharacterView sample=new CharacterView(this);sample.setAvatar(selected);sample.setEmotion(emotion,false);cell.addView(sample,new LinearLayout.LayoutParams(Math.round(64*density),Math.round(88*density)));
                TextView label=new TextView(this);label.setText(emotion.label());label.setTextColor(Color.WHITE);label.setTextSize(14);label.setPadding(0,0,0,16);cell.addView(label);
            }
        }
        button("Back",this::home);
    }
    private void liveAnalysis(){
        page("Live analysis (USB demo)");liveScreen=true;
        text("Optional processing on your connected computer",20);
        text("After you opt in and start a session, TEMPER reads up to eight fully visible plain-text turns (at most 1000 characters each) from one fictional WhatsApp chat you select. It sends only message text and LOCAL/REMOTE roles through the USB connection to this computer's local TEMPER backend for model analysis. No contact names, timestamps, drafts or full history are sent. No raw chat text is saved or logged on the phone or backend. Text is held in memory while the request runs; pause cannot recall a request already sent.",16);
        text("The session ends when you leave WhatsApp, select a different supported chat, pause or revoke access. Keyboard movement and temporarily unsupported layouts keep the selection active; analysis resumes automatically when readable text returns. Drag the companion to move it. Current support requires at least three complete text turns with both roles visible; documents, quoted replies and reactions make analysis unavailable. Estimates do not establish anyone's feelings. The debug connection is limited to 127.0.0.1:8080 over USB.",16);
        ConsentStore base=new ConsentStore(this);boolean opted=base.preferences().getInt("liveConsentVersion",0)==1;
        text("USB connection: "+(dev.temper.android.api.LocalAnalysisClient.configured(this)?"Configured":"Not configured"),16);
        text("Live session: "+dev.temper.android.accessibility.LiveCaptureState.status(),16);
        if(!opted){
            CheckBox agree=new CheckBox(this);agree.setText("I opt in to processing the selected fictional chat on this computer");agree.setTextColor(Color.WHITE);content.addView(agree);
            Button accept=new Button(this);accept.setText("Save live-processing consent");accept.setEnabled(false);agree.setOnCheckedChangeListener((view,checked)->accept.setEnabled(checked));accept.setOnClickListener(view->{dev.temper.android.privacy.LiveConsent.accept(this);liveAnalysis();});content.addView(accept);
        }else{
            if(dev.temper.android.privacy.LiveConsent.allowed(this)&&serviceEnabled()&&TemperAccessibilityService.connected()&&dev.temper.android.api.LocalAnalysisClient.configured(this))button("Start selected fictional chat",()->{ProbeState.clear();ParseProbeState.clear(this);dev.temper.android.accessibility.LiveCaptureState.arm(this);liveAnalysis();});
            else text("Resume TEMPER, connect its service under Accessibility and consent, and configure USB before starting. If Android shows the service enabled but TEMPER reports disconnected after an update/test, turn Use TEMPER off and on.",16);
            button("Stop and pause TEMPER",()->{base.pause(true);liveAnalysis();});
            button("Revoke live-processing consent",()->{dev.temper.android.privacy.LiveConsent.revoke(this);liveAnalysis();});
        }
        button("Refresh live status",this::liveAnalysis);button("Back",this::home);
    }
    private void account(){
        page("Device account");text("This demo account stays on this phone. It does not create a cloud account or secure the analysis server.",16);
        long version=pageVersion;accountWorker.execute(()->{try{String session=accountStore.session();runOnUiThread(()->{if(!isDestroyed()&&version==pageVersion)accountForm(session);});}catch(Exception failure){runOnUiThread(()->{if(!isDestroyed()&&version==pageVersion){text("Account storage unavailable. No session is active.",16);button("Back",this::home);}});}});
    }
    private void accountForm(String session){
        if(session!=null){text("Signed in on this device as "+session,18);button("Sign out",()->accountAction(()->{accountStore.signOut();new ConsentStore(this).pause(true);}));button("Back",this::home);return;}
        text("No current session",18);
        EditText username=new EditText(this);username.setHint("Username");username.setTextColor(Color.WHITE);username.setSingleLine(true);content.addView(username);
        EditText password=new EditText(this);password.setHint("Password (10–128 characters)");password.setTextColor(Color.WHITE);password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);content.addView(password);
        button("Create account",()->{String name=username.getText().toString();char[] secret=new char[password.length()];password.getText().getChars(0,secret.length,secret,0);password.setText("");accountAction(()->accountStore.create(name,secret));});
        button("Sign in",()->{String name=username.getText().toString();char[] secret=new char[password.length()];password.getText().getChars(0,secret.length,secret,0);password.setText("");accountAction(()->accountStore.signIn(name,secret));});button("Back",this::home);
    }
    private interface AccountAction {void execute() throws Exception;}
    private void accountAction(AccountAction action){
        long version=pageVersion;for(int i=0;i<content.getChildCount();i++)content.getChildAt(i).setEnabled(false);
        accountWorker.execute(()->{String error=null;try{action.execute();}catch(Exception failure){error=failure instanceof IllegalArgumentException?failure.getMessage():"Account storage unavailable";}String message=error;
            runOnUiThread(()->{if(isDestroyed()||version!=pageVersion)return;if(message==null)account();else{for(int i=0;i<content.getChildCount();i++)content.getChildAt(i).setEnabled(true);Toast.makeText(this,message,Toast.LENGTH_LONG).show();}});
        });
    }
}
