package dev.temper.android.analytics;

import android.content.Context;
import android.graphics.*;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import dev.temper.android.character.Emotion;

/** One compact eight-row spectrum, with explicit missing values rather than invented scores. */
public final class SpectrumView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private OverlaySummary summary=OverlaySummary.unavailable();
    private final float density,scale;
    private final TextPaint titlePaint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private StaticLayout title;
    private float headerHeight,rowHeight,labelWidth,valueWidth;
    private boolean stacked;
    public SpectrumView(Context context){super(context);density=getResources().getDisplayMetrics().density;scale=getResources().getDisplayMetrics().scaledDensity;titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));titlePaint.setTextSize(13*scale);titlePaint.setColor(0xfff4e8f8);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);bind(summary);}
    public void bind(OverlaySummary value){summary=value;StringBuilder spoken=new StringBuilder("Emotional spectrum. ");float[] values=value.spectrum();for(Emotion emotion:Emotion.values())spoken.append(emotion.label()).append(value.available()?": "+Math.round(values[emotion.ordinal()]*100)+" percent. ":": unavailable. ");setContentDescription(spoken);invalidate();}
    @Override protected void onMeasure(int width,int height){
        int available=MeasureSpec.getSize(width);title=StaticLayout.Builder.obtain("Emotional spectrum",0,18,titlePaint,Math.max(1,available)).setIncludePad(false).build();headerHeight=Math.max(26*scale,title.getHeight()+8*density);paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(12*scale);labelWidth=0;for(Emotion emotion:Emotion.values())labelWidth=Math.max(labelWidth,paint.measureText(emotion.label()));labelWidth+=10*density;valueWidth=paint.measureText("100%");stacked=available<labelWidth+valueWidth+24*density;rowHeight=(stacked?32:22)*scale;setMeasuredDimension(resolveSize(available,width),resolveSize(Math.round(headerHeight+Emotion.values().length*rowHeight),height));
    }
    @Override protected void onDraw(Canvas canvas){
        if(title!=null)title.draw(canvas);
        paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(12*scale);float left=stacked?0:labelWidth,right=getWidth()-valueWidth-8*density;
        float[] values=summary.spectrum();for(Emotion emotion:Emotion.values()){
            int index=emotion.ordinal();float top=headerHeight+index*rowHeight,barTop=top+(stacked?18:3)*scale,barBottom=top+(stacked?27:12)*scale;paint.setColor(0xffe4dbe9);canvas.drawText(emotion.label(),0,top+12*scale,paint);
            if(right>left){paint.setColor(dev.temper.android.home.HomeTokens.ELEVATED);canvas.drawRoundRect(left,barTop,right,barBottom,3*density,3*density,paint);if(summary.available()){paint.setColor(dev.temper.android.home.HomeTokens.ACCENT);canvas.drawRoundRect(left,barTop,left+(right-left)*values[index],barBottom,3*density,3*density,paint);}}
            paint.setColor(0xffe4dbe9);paint.setTextAlign(Paint.Align.RIGHT);canvas.drawText(summary.available()?Math.round(values[index]*100)+"%":"—",getWidth(),barBottom,paint);paint.setTextAlign(Paint.Align.LEFT);
        }
    }
}
