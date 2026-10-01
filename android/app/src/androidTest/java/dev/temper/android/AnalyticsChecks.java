package dev.temper.android;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.widget.TextView;
import dev.temper.android.analytics.*;
import dev.temper.android.adapters.ScreenObservation.Bounds;
import dev.temper.android.overlay.PopupPlacement;
import java.io.*;

final class AnalyticsChecks {
    static void run(Context context)throws Exception{
        float[] values={.1f,.05f,.65f,.1f,.1f,.25f,.05f,.05f};OverlaySummary summary=new OverlaySummary("Fictional concern","Fictional tension rising",values,true);values[0]=1;
        if(summary.spectrum()[0]!=.1f)throw new AssertionError("Spectrum input not isolated");float[] copy=summary.spectrum();copy[1]=1;if(summary.spectrum()[1]!=.05f)throw new AssertionError("Spectrum output not isolated");
        try{new OverlaySummary("state","direction",new float[]{Float.NaN,0,0,0,0,0,0,0},true);throw new AssertionError("Non-finite scores accepted");}catch(IllegalArgumentException expected){}
        var viewport=new Bounds(0,103,1080,2352);var character=new Bounds(0,1954,192,2218);var popup=PopupPlacement.place(viewport,character,840,830,12);
        if(!viewport.contains(popup)||popup.bottom()>=character.top())throw new AssertionError("Popup overlaps character/composer");
        var keyboard=PopupPlacement.place(viewport,new Bounds(0,1130,192,1394),840,830,12);if(keyboard.bottom()!=1118)throw new AssertionError("Popup did not track raised composer");
        try{PopupPlacement.place(viewport,new Bounds(0,500,192,764),840,830,12);throw new AssertionError("Oversized popup accepted");}catch(IllegalArgumentException expected){}
        AnalyticsPanel panel=new AnalyticsPanel(context);panel.bind(summary);
        if(panel.getChildCount()!=3||!(panel.getChildAt(0) instanceof TextView)||!(panel.getChildAt(1) instanceof TextView)||!(panel.getChildAt(2) instanceof SpectrumView))throw new AssertionError("Popup content exceeds two summaries and one graph");
        panel.measure(View.MeasureSpec.makeMeasureSpec(840,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));panel.layout(0,0,840,panel.getMeasuredHeight());
        Bitmap bitmap=Bitmap.createBitmap(840,panel.getHeight(),Bitmap.Config.ARGB_8888);panel.draw(new Canvas(bitmap));try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),"analytics-preview.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
        panel.bind(OverlaySummary.unavailable());String spoken=panel.getChildAt(2).getContentDescription().toString();if(spoken.contains("percent")||!spoken.contains("unavailable"))throw new AssertionError("Missing analysis presented as estimates");
    }
}
