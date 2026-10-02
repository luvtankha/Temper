package dev.temper.android.inference;

import dev.temper.android.adapters.VisibleConversation;
import dev.temper.android.analytics.OverlaySummary;
import dev.temper.android.api.LocalAnalysisClient;
import dev.temper.android.character.Emotion;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.json.*;

/** Small learned direction head. Synthetic-pilot scores are not emotional confidence. */
public final class ConversationContextModel {
    private static final Pattern QUOTES=Pattern.compile("\"[^\"\\n]*\"|(?<!\\w)'[^'\\n]+'(?!\\w)");
    private static final Pattern NON_WORD=Pattern.compile("[^a-z0-9]+");
    private final List<List<String>> groups=new ArrayList<>();
    private final String[] labels;
    private final double[][] weights;
    private final double[] bias;
    private final double minimum,margin;
    private final String baseHash;
    public boolean compatible(String hash){return baseHash.equals(hash);}
    public ConversationContextModel(InputStream input)throws Exception{
        JSONObject json=new JSONObject(new String(BoundedIo.read(input,100_000),StandardCharsets.UTF_8));
        if(json.getInt("schema")!=1||!json.getBoolean("approvedPilot"))throw new IllegalArgumentException("Invalid context model");
        baseHash=json.getString("baseModelSha256");if(!baseHash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid base model identity");
        JSONArray raw=json.getJSONArray("groups");if(raw.length()!=20)throw new IllegalArgumentException("Invalid context features");
        for(int i=0;i<raw.length();i++){List<String> phrases=new ArrayList<>();JSONArray items=raw.getJSONArray(i);for(int j=0;j<items.length();j++)phrases.add(" "+items.getString(j)+" ");groups.add(List.copyOf(phrases));}
        JSONArray names=json.getJSONArray("labels"),coefficients=json.getJSONArray("weights"),intercepts=json.getJSONArray("bias");
        if(names.length()!=5||coefficients.length()!=5||intercepts.length()!=5)throw new IllegalArgumentException("Invalid direction labels");
        labels=new String[5];weights=new double[5][groups.size()*5+16];bias=new double[5];
        Set<String> expected=new HashSet<>(Set.of("DEESCALATING","STABLE","TENSION_RISING","UNRESOLVED","WITHDRAWAL"));
        for(int i=0;i<5;i++){labels[i]=names.getString(i);if(!expected.remove(labels[i]))throw new IllegalArgumentException("Invalid direction label");JSONArray row=coefficients.getJSONArray(i);if(row.length()!=weights[i].length)throw new IllegalArgumentException("Invalid weights");for(int j=0;j<row.length();j++){weights[i][j]=row.getDouble(j);if(!Double.isFinite(weights[i][j]))throw new IllegalArgumentException("Invalid coefficient");}bias[i]=intercepts.getDouble(i);if(!Double.isFinite(bias[i]))throw new IllegalArgumentException("Invalid bias");}
        minimum=json.getDouble("minimum");margin=json.getDouble("margin");
        if(!Double.isFinite(minimum)||minimum<=0||minimum>1||!Double.isFinite(margin)||margin<0||margin>1)throw new IllegalArgumentException("Invalid prediction thresholds");
    }
    private double[] cues(String text){
        text=text.toLowerCase(Locale.ROOT).replace('\u2019','\'').replace('\u201c','"').replace('\u201d','"');
        text=" "+NON_WORD.matcher(QUOTES.matcher(text).replaceAll(" ")).replaceAll(" ").trim()+" ";
        double[] result=new double[groups.size()];for(int i=0;i<groups.size();i++)for(String phrase:groups.get(i))if(text.contains(phrase)){result[i]=1;break;}return result;
    }
    public double[] features(VisibleConversation snapshot,float[] current,float[] previous){
        validateSpectrum(current);if(previous!=null)validateSpectrum(previous);
        List<Integer> remote=new ArrayList<>();for(int i=0;i<snapshot.turns().size();i++)if(snapshot.turns().get(i).role()==VisibleConversation.Role.REMOTE)remote.add(i);
        if(remote.isEmpty())throw new IllegalArgumentException("No incoming evidence");
        int last=remote.get(remote.size()-1),prior=remote.size()>1?remote.get(remote.size()-2):-1,n=groups.size();
        double[] now=cues(snapshot.turns().get(last).text()),before=prior<0?new double[n]:cues(snapshot.turns().get(prior).text()),local=new double[n],older=new double[n];
        for(int i=prior+1;i<last;i++)if(snapshot.turns().get(i).role()==VisibleConversation.Role.LOCAL){double[] item=cues(snapshot.turns().get(i).text());for(int j=0;j<n;j++)local[j]=Math.max(local[j],item[j]);}
        if(remote.size()>2)for(int k=0;k<remote.size()-2;k++){double[] item=cues(snapshot.turns().get(remote.get(k)).text());for(int j=0;j<n;j++)older[j]+=item[j]/(remote.size()-2);}
        double[] result=new double[5*n+16];for(int i=0;i<n;i++){result[i]=now[i];result[n+i]=before[i];result[2*n+i]=local[i];result[3*n+i]=older[i];result[4*n+i]=now[i]-before[i];}
        for(int i=0;i<8;i++){result[5*n+i]=current[i];result[5*n+8+i]=previous==null?0:previous[i];}return result;
    }
    public double[] probabilities(double[] features){
        if(features.length!=weights[0].length)throw new IllegalArgumentException("Invalid features");
        for(double value:features)if(!Double.isFinite(value))throw new IllegalArgumentException("Invalid feature value");
        double[] scores=bias.clone();double max=Double.NEGATIVE_INFINITY,total=0;
        for(int i=0;i<scores.length;i++){for(int j=0;j<features.length;j++)scores[i]+=weights[i][j]*features[j];if(!Double.isFinite(scores[i]))throw new IllegalArgumentException("Invalid direction output");max=Math.max(max,scores[i]);}
        for(int i=0;i<scores.length;i++){scores[i]=Math.exp(scores[i]-max);total+=scores[i];}for(int i=0;i<scores.length;i++)scores[i]/=total;return scores;
    }
    private static void validateSpectrum(float[] values){
        if(values==null||values.length!=8)throw new IllegalArgumentException("Invalid spectrum");
        for(float value:values)if(!Float.isFinite(value)||value<0||value>1)throw new IllegalArgumentException("Invalid spectrum");
    }
    public String predict(double[] features){
        double[] scores=probabilities(features);int best=0,second=1;if(scores[second]>scores[best]){best=1;second=0;}
        for(int i=2;i<scores.length;i++)if(scores[i]>scores[best]){second=best;best=i;}else if(scores[i]>scores[second])second=i;
        return scores[best]>=minimum&&scores[best]-scores[second]>=margin?labels[best]:"UNCERTAIN";
    }
    public LocalAnalysisClient.Result summarize(VisibleConversation snapshot,float[] current,float[] previous){
        var base=EmotionEstimate.summarize(current,previous);
        int remote=0,local=0,pendingLocal=0;for(var turn:snapshot.turns())if(turn.role()==VisibleConversation.Role.REMOTE){remote++;local+=pendingLocal;pendingLocal=0;}else pendingLocal++;
        // A spectrum is useful from one verified incoming turn. Direction needs context.
        if(remote<2||local<1)return EmotionEstimate.summarize(current,null);
        String direction=predict(features(snapshot,current,previous)),state=base.summary().currentState(),text;
        Emotion character=base.emotion();
        switch(direction){
            case "TENSION_RISING" -> {state="Conflict cues increasing";text="Exchange may be becoming more tense";character=Emotion.CONCERNED;}
            case "DEESCALATING" -> {state="Signs of repair or softening";text="Tension may be easing; concerns can remain";}
            case "WITHDRAWAL" -> {state="Possible disengagement";text="Language may signal pulling back";character=Emotion.SAD;}
            case "UNRESOLVED" -> {state="Concerns appear unresolved";text="Agreement or repair is not clear yet";character=Emotion.CONCERNED;}
            case "STABLE" -> text="No clear interpersonal escalation detected";
            default -> text="Direction uncertain • mixed context";
        }
        return new LocalAnalysisClient.Result(new OverlaySummary(state,text,current,true),character,direction,"UNCERTAIN".equals(direction)?.2:.5);
    }
}
