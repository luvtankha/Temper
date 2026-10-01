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
        text("This build can detect WhatsApp and inspect one user-armed chat layout after informed opt-in. No message text is read or sent yet.",16);
        button("Accessibility and consent",this::consent);button("Device account",this::account);button("Settings",this::settings);button("Fictional preview",this::preview);
    }
    private void settings(){
        page("Settings");text("Chat capture and overlay are not implemented yet. Pause also stops supported-app detection.",16);
        Switch pause=new Switch(this);pause.setText("Keep TEMPER paused");pause.setTextColor(Color.WHITE);pause.setChecked(getPreferences(MODE_PRIVATE).getBoolean("paused",true));
        pause.setOnCheckedChangeListener((view,checked)->new ConsentStore(this).pause(checked));content.addView(pause);
        text("No chat text is stored or transmitted. The pause preference is local to this device.",16);button("Reset pause preference",()->{new ConsentStore(this).pause(true);settings();});button("Back",this::home);
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
        text("Test inspection is limited to a fictional chat you select. The current one-shot probe reads resource identifiers and bounds, never text or descriptions. The upcoming adapter may read up to eight visible test-chat messages (1000 characters each) and a conversation identifier, keeping only a short window in memory. No composer draft, contact list or unrelated content is read; no raw chats are logged. Pause clears the window. Server transmission requires a separate future consent.",16);
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
            if(!store.paused()&&serviceEnabled())button("Arm next fictional chat layout",()->{LayoutMetadata.clear(this);ProbeState.arm();consent();});
        }
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
    private void preview(){page("Fictional preview");text("Neutral companion placeholder",22);text("This screen is a development preview only. It does not read WhatsApp, run live analysis or draw over other apps.",16);button("Back",this::home);}
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
