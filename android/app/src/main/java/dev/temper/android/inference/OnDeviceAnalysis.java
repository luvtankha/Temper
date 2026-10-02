package dev.temper.android.inference;

import android.content.Context;
import ai.onnxruntime.*;
import dev.temper.android.adapters.VisibleConversation;
import dev.temper.android.api.LocalAnalysisClient;
import java.io.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import dev.temper.android.adapters.WhatsAppAdapter;

/** CPU model session owned by the bounded live worker. No network calls and no captured-text cache. */
public final class OnDeviceAnalysis implements AutoCloseable {
    private OrtSession session;
    private RobertaTokenizer tokenizer;
    private OrtEnvironment environment;
    private ConversationContextModel contextModel;
    private static final Pattern URL=Pattern.compile("https?://\\S+",Pattern.CASE_INSENSITIVE),MENTION=Pattern.compile("(?<!\\w)@\\w+");
    private final LinkedHashMap<String,float[]> scores=new LinkedHashMap<>(8,.75f,true);
    private String conversation;
    private volatile long contextEpoch;
    private long loadedEpoch=-1;
    /** Only hashed keys and 8 floats per entry. No captured text is retained. */
    public void forgetConversation(){synchronized(scores){contextEpoch++;scores.clear();}}
    public void prepare(Context context)throws Exception{
        if(session!=null)return;
        try(InputStream input=context.getAssets().open("emotion/context-model.json")){contextModel=new ConversationContextModel(input);}
        ModelFiles.verifyInstalled(context);environment=OrtEnvironment.getEnvironment();
        try(InputStream compiled=context.getAssets().open("emotion/tokenizer.bin")){tokenizer=new RobertaTokenizer(compiled);}
        try(var options=new OrtSession.SessionOptions()){options.setIntraOpNumThreads(2);options.setInterOpNumThreads(1);session=environment.createSession(ModelFiles.file(context).getAbsolutePath(),options);}
    }
    public LocalAnalysisClient.Result analyze(Context context,VisibleConversation snapshot,BooleanSupplier active)throws Exception{
        if(!active.getAsBoolean())throw new IllegalStateException("Session stopped");
        prepare(context);
        synchronized(scores){long epoch=contextEpoch;if(epoch!=loadedEpoch||!snapshot.conversationKey().equals(conversation)){scores.clear();conversation=snapshot.conversationKey();loadedEpoch=epoch;}}

        List<VisibleConversation.Turn> remote=snapshot.turns().stream().filter(turn->turn.role()==VisibleConversation.Role.REMOTE).toList();if(remote.isEmpty())return new LocalAnalysisClient.Result(dev.temper.android.analytics.OverlaySummary.unavailable(),dev.temper.android.character.Emotion.NEUTRAL,"UNCERTAIN",0);
        float[] current=classify(remote.get(remote.size()-1).text(),active);float[] previous=remote.size()<2?null:classify(remote.get(remote.size()-2).text(),active);if(!active.getAsBoolean())throw new IllegalStateException("Session stopped");return contextModel.compatible(dev.temper.android.BuildConfig.EMOTION_HASH)?contextModel.summarize(snapshot,current,previous):EmotionEstimate.summarize(current,previous);
    }
    private float[] classify(String text,BooleanSupplier active)throws Exception{
        if(!active.getAsBoolean())throw new IllegalStateException("Session stopped");String normalized=normalizedInput(text);String key=WhatsAppAdapter.hash(normalized);long epoch=contextEpoch;synchronized(scores){float[] cached=scores.get(key);if(cached!=null)return cached;}long[] tokens=tokenizer.encode(normalized,128),mask=new long[tokens.length];Arrays.fill(mask,1);
        try(var input=OnnxTensor.createTensor(environment,new long[][]{tokens});var attention=OnnxTensor.createTensor(environment,new long[][]{mask});var result=session.run(Map.of("input_ids",input,"attention_mask",attention))){float[] value=EmotionEstimate.spectrum(((float[][])result.get(0).getValue())[0]);if(!active.getAsBoolean())throw new IllegalStateException("Session stopped");synchronized(scores){if(epoch==contextEpoch){scores.put(key,value);if(scores.size()>8)scores.remove(scores.keySet().iterator().next());}}return value;}
    }
    static String normalizedInput(String text){
        String normalized=MENTION.matcher(URL.matcher(text).replaceAll("http")).replaceAll("@user");
        // A short handle can grow during normalization. Preserve the tokenizer's
        // existing input bound and avoid cutting a UTF-16 surrogate pair.
        int end=Math.min(normalized.length(),1000);
        if(end<normalized.length()&&Character.isHighSurrogate(normalized.charAt(end-1))&&Character.isLowSurrogate(normalized.charAt(end)))end--;
        return normalized.substring(0,end);
    }
    @Override public void close(){forgetConversation();conversation=null;contextModel=null;loadedEpoch=-1;if(session!=null){try{session.close();}catch(OrtException ignored){}session=null;tokenizer=null;}}
}
