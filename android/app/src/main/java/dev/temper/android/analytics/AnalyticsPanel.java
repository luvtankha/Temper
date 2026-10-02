package dev.temper.android.analytics;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;

/** Exactly two short summaries and one spectrum chart. */
public final class AnalyticsPanel extends LinearLayout {
    private final TextView current,direction;
    private final SpectrumView spectrum;
    public AnalyticsPanel(Context context){
        super(context);setOrientation(VERTICAL);int pad=Math.round(12*getResources().getDisplayMetrics().density);setPadding(pad,pad,pad,pad);
        GradientDrawable background=new GradientDrawable();background.setColor(dev.temper.android.home.HomeTokens.SURFACE);background.setCornerRadius(pad);background.setStroke(Math.max(1,pad/12),dev.temper.android.home.HomeTokens.BORDER);setBackground(background);
        current=text(context);direction=text(context);spectrum=new SpectrumView(context);addView(spectrum,new LayoutParams(-1,-2));bind(OverlaySummary.unavailable());
    }
    private TextView text(Context context){TextView view=new TextView(context);view.setTextSize(14);view.setTextColor(Color.WHITE);view.setMaxLines(2);view.setEllipsize(android.text.TextUtils.TruncateAt.END);view.setPadding(0,0,0,Math.round(6*getResources().getDisplayMetrics().density));addView(view,new LayoutParams(-1,-2));return view;}
    public void bind(OverlaySummary summary){current.setText("Current state: "+summary.currentState());direction.setText("Direction: "+summary.direction());spectrum.bind(summary);}
}
