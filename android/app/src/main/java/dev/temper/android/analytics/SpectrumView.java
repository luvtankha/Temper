package dev.temper.android.analytics;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import dev.temper.android.character.Emotion;

/** One compact eight-row spectrum, with explicit missing values rather than invented scores. */
public final class SpectrumView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private OverlaySummary summary=OverlaySummary.unavailable();
    private final float density,scale;
    public SpectrumView(Context context){super(context);density=getResources().getDisplayMetrics().density;scale=getResources().getDisplayMetrics().scaledDensity;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);bind(summary);}
    public void bind(OverlaySummary value){summary=value;StringBuilder spoken=new StringBuilder("Emotional spectrum. ");float[] values=value.spectrum();for(Emotion emotion:Emotion.values())spoken.append(emotion.label()).append(value.available()?": "+Math.round(values[emotion.ordinal()]*100)+" percent. ":": unavailable. ");setContentDescription(spoken);invalidate();}
    @Override protected void onMeasure(int width,int height){setMeasuredDimension(MeasureSpec.getSize(width),Math.round(26*scale+8*22*scale));}
    @Override protected void onDraw(Canvas canvas){
        paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));paint.setTextSize(13*scale);paint.setColor(0xfff4e8f8);canvas.drawText("Emotional spectrum",0,15*scale,paint);
        paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(12*scale);float labelWidth=paint.measureText("Frustrated")+10*density;float right=getWidth()-paint.measureText("100%")-8*density;
        float[] values=summary.spectrum();for(Emotion emotion:Emotion.values()){
            int index=emotion.ordinal();float top=26*scale+index*22*scale;paint.setColor(0xffe4dbe9);canvas.drawText(emotion.label(),0,top+12*scale,paint);
            if(right>labelWidth){paint.setColor(0xff49364e);canvas.drawRoundRect(labelWidth,top+3*scale,right,top+12*scale,3*density,3*density,paint);if(summary.available()){paint.setColor(0xffdc75da);canvas.drawRoundRect(labelWidth,top+3*scale,labelWidth+(right-labelWidth)*values[index],top+12*scale,3*density,3*density,paint);}}
            paint.setColor(0xffe4dbe9);canvas.drawText(summary.available()?Math.round(values[index]*100)+"%":"—",right+8*density,top+12*scale,paint);
        }
    }
}
