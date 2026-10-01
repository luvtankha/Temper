package dev.temper.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.Color;

/** Debug-only, fictional input surface. No capture or message transport. */
public final class DebugOverlayActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(24,96,24,24);layout.setBackgroundColor(Color.rgb(21,16,25));TextView title=new TextView(this);title.setText("TEMPER fictional overlay test");title.setTextColor(Color.WHITE);title.setTextSize(24);layout.addView(title);TextView note=new TextView(this);note.setText("Dummy screen only. The floating panel must show no fabricated analysis here. Nothing is sent or read from another app.");note.setTextColor(Color.WHITE);layout.addView(note);Space space=new Space(this);layout.addView(space,new LinearLayout.LayoutParams(-1,0,1));EditText input=new EditText(this);input.setHint("Dummy message box (no messages sent)");input.setTextColor(Color.WHITE);input.setHintTextColor(0xffd7bfe2);layout.addView(input);setContentView(layout);}
}
