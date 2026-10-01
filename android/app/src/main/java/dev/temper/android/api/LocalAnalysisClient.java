package dev.temper.android.api;

import android.content.Context;
import dev.temper.android.adapters.VisibleConversation;
import dev.temper.android.analytics.OverlaySummary;
import dev.temper.android.character.Emotion;
import dev.temper.android.privacy.LiveConsent;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** USB-forwarded loopback demo endpoint. No redirects or arbitrary upload destinations. */
public final class LocalAnalysisClient {
    public record Result(OverlaySummary summary,Emotion emotion,String trajectory,double evidenceStrength){}
    public static boolean configured(Context context){try{return token(context)!=null;}catch(Exception missing){return false;}}
    private static String token(Context context)throws Exception{
        File file=new File(context.getFilesDir(),"overlay-connection.json");if(!file.isFile()||file.length()>1024)return null;
        JSONObject config=new JSONObject(new String(java.nio.file.Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));String token=config.getString("token");return token.matches("[a-f0-9]{64}")?token:null;
    }
    public static Result parse(String body)throws Exception{
        JSONObject json=new JSONObject(body);if(!json.getBoolean("available"))return new Result(OverlaySummary.unavailable(),Emotion.NEUTRAL,"UNCERTAIN",0);
        JSONArray raw=json.getJSONArray("spectrum");if(raw.length()!=8)throw new IllegalArgumentException("Invalid spectrum");float[] values=new float[8];for(int i=0;i<8;i++)values[i]=(float)raw.getDouble(i);
        String trajectory=json.getString("trajectory");if(!java.util.Set.of("STABLE","TENSION_RISING","ESCALATION_RISK","DEESCALATING","WITHDRAWAL_RISK","REPAIR_OPPORTUNITY").contains(trajectory))throw new IllegalArgumentException("Unknown trajectory");
        double strength=json.getDouble("evidenceStrength");if(!Double.isFinite(strength)||strength<0||strength>1)throw new IllegalArgumentException("Invalid evidence");
        return new Result(new OverlaySummary(json.getString("currentState"),json.getString("direction"),values,true),Emotion.valueOf(json.getString("character")),trajectory,strength);
    }
    public Result analyze(Context context,VisibleConversation snapshot,java.util.function.BooleanSupplier active)throws Exception{
        if(snapshot.status()!=VisibleConversation.Status.AVAILABLE||!LiveConsent.allowed(context)||!active.getAsBoolean())throw new IllegalStateException("Live analysis unavailable");
        String token=token(context);if(token==null)throw new IllegalStateException("Connection missing");
        JSONArray turns=new JSONArray();for(var turn:snapshot.turns())turns.put(new JSONObject().put("role",turn.role().name()).put("text",turn.text()));
        byte[] body=new JSONObject().put("turns",turns).toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection=(HttpURLConnection)new URL("http://127.0.0.1:8080/api/overlay/analyze").openConnection();
        try{
            connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(4000);connection.setReadTimeout(45000);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setFixedLengthStreamingMode(body.length);connection.setRequestProperty("Content-Type","application/json; charset=utf-8");connection.setRequestProperty("Authorization","Bearer "+token);
            if(!LiveConsent.allowed(context)||!active.getAsBoolean())throw new IllegalStateException("Session stopped");
            try(OutputStream output=connection.getOutputStream()){if(!LiveConsent.allowed(context)||!active.getAsBoolean())throw new IllegalStateException("Session stopped");output.write(body);}if(connection.getResponseCode()!=200)throw new IOException("Analysis unavailable");
            try(InputStream input=connection.getInputStream();ByteArrayOutputStream output=new ByteArrayOutputStream()){
                byte[] buffer=new byte[2048];int count;while((count=input.read(buffer))!=-1){if(!active.getAsBoolean()||!LiveConsent.allowed(context)||output.size()+count>65536)throw new IOException("Analysis response unavailable");output.write(buffer,0,count);}return parse(output.toString(StandardCharsets.UTF_8.name()));
            }
        }finally{java.util.Arrays.fill(body,(byte)0);connection.disconnect();}
    }
}
