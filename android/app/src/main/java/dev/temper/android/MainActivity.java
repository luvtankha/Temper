package dev.temper.android;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;

/** Onboarding shell only; no host-app access or permissions. */
public final class MainActivity extends Activity {
    private LinearLayout content;
    @Override public void onCreate(Bundle state){super.onCreate(state);home();}
    private void page(String title){
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);int pad=(int)(24*getResources().getDisplayMetrics().density);content.setPadding(pad,pad*2,pad,pad);content.setBackgroundColor(Color.rgb(21,16,25));scroll.addView(content);setContentView(scroll);text(title,28);
    }
    private void text(String value,int size){TextView text=new TextView(this);text.setText(value);text.setTextColor(Color.rgb(242,230,248));text.setTextSize(size);text.setPadding(0,12,0,12);content.addView(text);}
    private void button(String label,Runnable action){Button button=new Button(this);button.setText(label);button.setOnClickListener(v->action.run());content.addView(button);}
    private void home(){
        page("TEMPER");text("A small companion for conversations",19);
        text("The Android overlay is in development. It will estimate language and direction in supported visible WhatsApp conversations. Estimates cannot establish another person's feelings.",16);
        text("Nothing is being captured. Accessibility access will require a separate informed opt-in before it is used.",16);
        button("Settings",this::settings);button("Fictional preview",this::preview);
    }
    private void settings(){
        page("Settings");text("Capture and overlay are unavailable in this foundation build.",16);
        Switch pause=new Switch(this);pause.setText("Keep TEMPER paused");pause.setTextColor(Color.WHITE);pause.setChecked(getPreferences(MODE_PRIVATE).getBoolean("paused",true));
        pause.setOnCheckedChangeListener((view,checked)->getPreferences(MODE_PRIVATE).edit().putBoolean("paused",checked).apply());content.addView(pause);
        text("No chat text is stored or transmitted. The pause preference is local to this device.",16);button("Clear local preferences",()->{getPreferences(MODE_PRIVATE).edit().clear().apply();settings();});button("Back",this::home);
    }
    private void preview(){page("Fictional preview");text("Neutral companion placeholder",22);text("This screen is a development preview only. It does not read WhatsApp, run live analysis or draw over other apps.",16);button("Back",this::home);}
}
