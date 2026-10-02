package dev.temper.android.analytics;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;

/** Exactly two short summaries and one spectrum chart. */
public final class AnalyticsPanel extends ScrollView {
    private final TextView current,direction;
    private final SpectrumView spectrum;
    private final LinearLayout body;
    public AnalyticsPanel(Context context){
        super(context);int pad=Math.round(12*getResources().getDisplayMetrics().density);setPadding(pad,pad,pad,pad);body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);addView(body,new ScrollView.LayoutParams(-1,-2));
        GradientDrawable background=new GradientDrawable();background.setColor(dev.temper.android.home.HomeTokens.SURFACE);background.setCornerRadius(pad);background.setStroke(Math.max(1,pad/12),dev.temper.android.home.HomeTokens.BORDER);setBackground(background);
        current=text(context);direction=text(context);spectrum=new SpectrumView(context);body.addView(spectrum,new LinearLayout.LayoutParams(-1,-2));bind(OverlaySummary.unavailable());
    }
    private TextView text(Context context){TextView view=new TextView(context);view.setTextSize(14);view.setTextColor(Color.WHITE);view.setPadding(0,0,0,Math.round(6*getResources().getDisplayMetrics().density));body.addView(view,new LinearLayout.LayoutParams(-1,-2));return view;}
    public void bind(OverlaySummary summary){current.setText("Current state: "+summary.currentState());direction.setText("Direction: "+summary.direction());spectrum.bind(summary);}
}
