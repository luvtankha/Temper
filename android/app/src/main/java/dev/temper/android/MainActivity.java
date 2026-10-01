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

/** Native onboarding, device account and explicit accessibility consent controls. */
public final class MainActivity extends Activity {
    private LinearLayout content;
    private final ExecutorService accountWorker=Executors.newSingleThreadExecutor();
    private DeviceAccountStore accountStore;
    private long pageVersion;
    private boolean consentScreen;
    private boolean liveScreen;
    @Override public void onResume(){super.onResume();if(consentScreen)consent();else if(liveScreen)liveAnalysis();}
    @Override public void onCreate(Bundle state){super.onCreate(state);accountStore=new DeviceAccountStore(this);home();}
    @Override public void onDestroy(){accountWorker.shutdown();super.onDestroy();}
    private void page(String title){
        pageVersion++;consentScreen=false;liveScreen=false;
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);int pad=(int)(24*getResources().getDisplayMetrics().density);content.setPadding(pad,pad*2,pad,pad);content.setBackgroundColor(Color.rgb(21,16,25));scroll.addView(content);setContentView(scroll);text(title,28);
    }
    private void text(String value,int size){TextView text=new TextView(this);text.setText(value);text.setTextColor(Color.rgb(242,230,248));text.setTextSize(size);text.setPadding(0,12,0,12);content.addView(text);}
    private void button(String label,Runnable action){Button button=new Button(this);button.setText(label);button.setOnClickListener(v->action.run());content.addView(button);}
    private void home(){
        page("TEMPER");text("A small companion for conversations",19);
        text("TEMPER places a small character above the WhatsApp message box. Tap it for estimated language, conversation direction and one emotion graph. This USB demo supports selected fictional chats on the documented WhatsApp build. Estimates cannot establish another person's feelings.",16);
        text("Test probes keep selected fictional chat text on this phone. Optional USB live analysis requires a separate opt-in to process the visible window on your connected computer.",16);
        button("Accessibility and consent",this::consent);button("Live analysis (USB demo)",this::liveAnalysis);button("Device account",this::account);button("Settings",this::settings);button("Fictional preview",this::preview);
    }
    private void settings(){
        page("Settings");text("The selected-chat character overlay supports optional USB live analysis with separate consent. Pause stops inspection and clears the visible context.",16);
        Switch pause=new Switch(this);pause.setText("Keep TEMPER paused");pause.setTextColor(Color.WHITE);pause.setChecked(getPreferences(MODE_PRIVATE).getBoolean("paused",true));
        pause.setOnCheckedChangeListener((view,checked)->new ConsentStore(this).pause(checked));content.addView(pause);
        text("No chat text is persisted. USB live analysis sends only a bounded selected-chat window after its separate opt-in. Clearing inspection data pauses TEMPER and removes test metadata. Removing the USB connection also revokes live-processing consent; your device account stays available.",16);
        button("Clear inspection data and pause",()->{dev.temper.android.privacy.PrivacyControls.clearInspection(this);settings();});
        button("Remove USB connection and pause",()->{try{dev.temper.android.privacy.PrivacyControls.removeConnection(this);settings();}catch(IllegalStateException failure){Toast.makeText(this,failure.getMessage(),Toast.LENGTH_LONG).show();}});
        button("Reset pause preference",()->{new ConsentStore(this).pause(true);settings();});button("Back",this::home);
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
        float density=getResources().getDisplayMetrics().density;CharacterView live=new CharacterView(this);
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
                CharacterView sample=new CharacterView(this);sample.setEmotion(emotion,false);cell.addView(sample,new LinearLayout.LayoutParams(Math.round(64*density),Math.round(88*density)));
                TextView label=new TextView(this);label.setText(emotion.label());label.setTextColor(Color.WHITE);label.setTextSize(14);label.setPadding(0,0,0,16);cell.addView(label);
            }
        }
        button("Back",this::home);
    }
    private void liveAnalysis(){
        page("Live analysis (USB demo)");liveScreen=true;
        text("Optional processing on your connected computer",20);
        text("After you opt in and start a session, TEMPER reads up to eight fully visible plain-text turns (at most 1000 characters each) from one fictional WhatsApp chat you select. It sends only message text and LOCAL/REMOTE roles through the USB connection to this computer's local TEMPER backend for model analysis. No contact names, timestamps, drafts or full history are sent. No raw chat text is saved or logged on the phone or backend. Text is held in memory while the request runs; pause cannot recall a request already sent.",16);
        text("The session ends when you leave the chat, pause, revoke, or the layout becomes unsupported. Current support requires at least three complete text turns with both roles visible; documents, quoted replies and reactions make analysis unavailable. Estimates do not establish anyone's feelings. The debug connection is limited to 127.0.0.1:8080 over USB.",16);
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
