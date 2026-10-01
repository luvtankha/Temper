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
    @Override public void onResume(){super.onResume();if(consentScreen)consent();}
    @Override public void onCreate(Bundle state){super.onCreate(state);accountStore=new DeviceAccountStore(this);home();}
    @Override public void onDestroy(){accountWorker.shutdown();super.onDestroy();}
    private void page(String title){
        pageVersion++;consentScreen=false;
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);int pad=(int)(24*getResources().getDisplayMetrics().density);content.setPadding(pad,pad*2,pad,pad);content.setBackgroundColor(Color.rgb(21,16,25));scroll.addView(content);setContentView(scroll);text(title,28);
    }
    private void text(String value,int size){TextView text=new TextView(this);text.setText(value);text.setTextColor(Color.rgb(242,230,248));text.setTextSize(size);text.setPadding(0,12,0,12);content.addView(text);}
    private void button(String label,Runnable action){Button button=new Button(this);button.setText(label);button.setOnClickListener(v->action.run());content.addView(button);}
    private void home(){
        page("TEMPER");text("A small companion for conversations",19);
        text("The Android overlay is in development. It will estimate language and direction in supported visible WhatsApp conversations. Estimates cannot establish another person's feelings.",16);
        text("This build can inspect one user-selected fictional WhatsApp chat after informed opt-in. Text stays in memory and is never sent to a server in this build.",16);
        button("Accessibility and consent",this::consent);button("Device account",this::account);button("Settings",this::settings);button("Fictional preview",this::preview);
    }
    private void settings(){
        page("Settings");text("The selected-chat character overlay is available for testing. Live analysis is still in development. Pause stops inspection and clears captured test data.",16);
        Switch pause=new Switch(this);pause.setText("Keep TEMPER paused");pause.setTextColor(Color.WHITE);pause.setChecked(getPreferences(MODE_PRIVATE).getBoolean("paused",true));
        pause.setOnCheckedChangeListener((view,checked)->new ConsentStore(this).pause(checked));content.addView(pause);
        text("No chat text is persisted or transmitted. The pause preference is local to this device.",16);button("Reset pause preference",()->{new ConsentStore(this).pause(true);settings();});button("Back",this::home);
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
        text("Test inspection is limited to a fictional one-to-one chat you select. The layout probe reads identifiers and bounds without text. The separately armed parser reads up to eight visible messages (1000 characters each), their timestamps and a conversation identifier, keeping text only in memory. No composer draft, contact list or unrelated content is read; no raw chats are logged or exported. Pause clears the window. Server transmission requires a separate future consent.",16);
        text("You control access: pause stops detection; revoke removes consent and disables the service. Android's Accessibility settings can also disable TEMPER at any time.",16);
        text("System service: "+(serviceEnabled()?"Enabled":"Disabled"),18);
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
