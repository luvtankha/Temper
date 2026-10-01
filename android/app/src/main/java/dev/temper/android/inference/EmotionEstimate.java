package dev.temper.android.inference;

import dev.temper.android.analytics.OverlaySummary;
import dev.temper.android.character.Emotion;
import dev.temper.android.api.LocalAnalysisClient;

/** Independent multi-label scores, not percentages summing to 100. Direction is a conservative proxy. */
public final class EmotionEstimate {
    private EmotionEstimate(){}
    public static float[] spectrum(float[] logits){
        if(logits.length!=28)throw new IllegalArgumentException("Invalid model output");float[] p=new float[28];for(int i=0;i<28;i++){if(!Float.isFinite(logits[i]))throw new IllegalArgumentException("Invalid model output");p[i]=(float)(1/(1+Math.exp(-logits[i])));}
        // GoEmotions label order: joy 17, fear 14, nervousness 19, annoyance 3, neutral 27.
        // Caring is excluded from the concern proxy: reassurance is not evidence of distress.
        return new float[]{p[27],p[17],Math.max(p[14],p[19]),p[6],p[25],p[3],p[2],p[26]};
    }
    private static float tension(float[] p){return Math.max(p[6],Math.max(p[5],p[2]));}
    public static LocalAnalysisClient.Result summarize(float[] current,float[] prior){
        if(current.length!=8)throw new IllegalArgumentException("Invalid spectrum");int dominant=0;for(int i=1;i<8;i++)if(current[i]>current[dominant])dominant=i;
        String currentText=current[dominant]<.35f?"Mixed or weak language signals":Emotion.values()[dominant].label()+" language estimated";
        String trajectory="UNCERTAIN",direction="Direction uncertain • limited evidence";Emotion character=current[dominant]<.35f?Emotion.NEUTRAL:Emotion.values()[dominant];
        if(prior!=null){float now=tension(current),before=tension(prior);if(now>=.65f&&now-before>=.15f){trajectory="TENSION_RISING";direction="Tension may be rising";character=Emotion.CONCERNED;}else if(before>=.5f&&before-now>=.2f){trajectory="DEESCALATING";direction="Language appears to be softening";}else if(now<.25f&&before<.25f){trajectory="STABLE";direction="No clear rise in tension";}else if(now>=.8f&&before>=.6f){trajectory="ESCALATION_RISK";direction="Strong tense language • caution";character=Emotion.FRUSTRATED;}}
        return new LocalAnalysisClient.Result(new OverlaySummary(currentText,direction,current,true),character,trajectory,prior==null?.2:.5);
    }
}
