package dev.temper.android;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.ViewGroup;
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
        var topCharacter=new Bounds(0,103,192,367);var below=PopupPlacement.place(viewport,topCharacter,840,830,12);if(below.top()!=379||!viewport.contains(below))throw new AssertionError("Top-dragged character cannot open popup below");
        try{PopupPlacement.place(viewport,new Bounds(0,500,192,764),840,2250,12);throw new AssertionError("Oversized popup accepted");}catch(IllegalArgumentException expected){}
        AnalyticsPanel panel=new AnalyticsPanel(context);panel.bind(summary);
        if(panel.getChildCount()!=1||!(panel.getChildAt(0) instanceof ViewGroup))throw new AssertionError("Compact panel scroll container missing");
        ViewGroup body=(ViewGroup)panel.getChildAt(0);
        if(body.getChildCount()!=3||!(body.getChildAt(0) instanceof TextView)||!(body.getChildAt(1) instanceof TextView)||!(body.getChildAt(2) instanceof SpectrumView))throw new AssertionError("Popup content exceeds two summaries and one graph");
        panel.measure(View.MeasureSpec.makeMeasureSpec(840,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));panel.layout(0,0,840,panel.getMeasuredHeight());
        Bitmap bitmap=Bitmap.createBitmap(840,panel.getHeight(),Bitmap.Config.ARGB_8888);panel.draw(new Canvas(bitmap));try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),"analytics-preview.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
        panel.bind(OverlaySummary.unavailable());String spoken=body.getChildAt(2).getContentDescription().toString();if(spoken.contains("percent")||!spoken.contains("unavailable"))throw new AssertionError("Missing analysis presented as estimates");
        for(var missing:new OverlaySummary[]{OverlaySummary.unavailable(),OverlaySummary.analyzing(),OverlaySummary.connectionUnavailable()}){
            panel.bind(missing);if(body.getChildAt(2).getContentDescription().toString().contains("percent")||body.getChildCount()!=3)throw new AssertionError("Failure status invented scores or added content");
            if(!((TextView)body.getChildAt(0)).getText().toString().equals("Current state: "+missing.currentState())||!((TextView)body.getChildAt(1)).getText().toString().equals("Direction: "+missing.direction()))throw new AssertionError("Short status mapping incorrect");
        }
        panel.bind(new OverlaySummary("Positive language","Appears stable",new float[]{0,.9f,0,0,0,0,0,0},true));
        if(!body.getChildAt(2).getContentDescription().toString().contains("Happy: 90 percent")||body.getChildAt(2).getContentDescription().toString().contains("unavailable"))throw new AssertionError("Live chart replacement did not update");
    }
}
