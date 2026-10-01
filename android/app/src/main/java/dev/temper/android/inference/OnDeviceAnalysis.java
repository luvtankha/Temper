package dev.temper.android.inference;

import android.content.Context;
import ai.onnxruntime.*;
import dev.temper.android.adapters.VisibleConversation;
import dev.temper.android.api.LocalAnalysisClient;
import java.io.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** CPU model session owned by the bounded live worker. No network calls and no captured-text cache. */
public final class OnDeviceAnalysis implements AutoCloseable {
    private OrtSession session;
    private RobertaTokenizer tokenizer;
    private OrtEnvironment environment;
    public LocalAnalysisClient.Result analyze(Context context,VisibleConversation snapshot,BooleanSupplier active)throws Exception{
        if(!active.getAsBoolean())throw new IllegalStateException("Session stopped");
        if(session==null){ModelFiles.verify(ModelFiles.file(context));environment=OrtEnvironment.getEnvironment();try(InputStream vocab=context.getAssets().open("emotion/vocab.json");InputStream merges=context.getAssets().open("emotion/merges.txt")){tokenizer=new RobertaTokenizer(vocab,merges);}try(var options=new OrtSession.SessionOptions()){options.setIntraOpNumThreads(2);options.setInterOpNumThreads(1);session=environment.createSession(ModelFiles.file(context).getAbsolutePath(),options);}}
        List<VisibleConversation.Turn> remote=snapshot.turns().stream().filter(turn->turn.role()==VisibleConversation.Role.REMOTE).toList();if(remote.isEmpty())return new LocalAnalysisClient.Result(dev.temper.android.analytics.OverlaySummary.unavailable(),dev.temper.android.character.Emotion.NEUTRAL,"UNCERTAIN",0);
        float[] current=classify(remote.get(remote.size()-1).text(),active);float[] previous=remote.size()<2?null:classify(remote.get(remote.size()-2).text(),active);if(!active.getAsBoolean())throw new IllegalStateException("Session stopped");return EmotionEstimate.summarize(current,previous);
    }
    private float[] classify(String text,BooleanSupplier active)throws Exception{
        if(!active.getAsBoolean())throw new IllegalStateException("Session stopped");String normalized=text.replaceAll("(?i)https?://\\S+","http").replaceAll("(?<!\\w)@\\w+","@user");long[] tokens=tokenizer.encode(normalized,128),mask=new long[tokens.length];Arrays.fill(mask,1);
        try(var input=OnnxTensor.createTensor(environment,new long[][]{tokens});var attention=OnnxTensor.createTensor(environment,new long[][]{mask});var result=session.run(Map.of("input_ids",input,"attention_mask",attention))){return EmotionEstimate.spectrum(((float[][])result.get(0).getValue())[0]);}
    }
    @Override public void close(){if(session!=null){try{session.close();}catch(OrtException ignored){}session=null;tokenizer=null;}}
}
