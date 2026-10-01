package dev.temper.android.learning;

import android.content.Context;
import java.net.*;
import java.nio.charset.StandardCharsets;
import dev.temper.android.inference.BoundedIo;

/** Explicit reviewed submissions only. A failure retains no automatic retry queue. */
public final class LearningClient {
    public void submit(Context context,String body)throws Exception{
        LearningConsent consent=new LearningConsent(context);if(!consent.accepted())throw new IllegalStateException("Contribution consent required");var review=LearningConsent.SESSION.review();if(review==null||!review.id().equals(new org.json.JSONObject(body).getString("sessionId")))throw new IllegalStateException("Session expired");byte[] data=body.getBytes(StandardCharsets.UTF_8);if(data.length>384_000)throw new IllegalArgumentException("Session too large");String token=consent.token(true);if(!consent.accepted())throw new IllegalStateException("Contribution consent withdrawn");request(LearningConsent.origin(),"/api/learning/sessions","POST",token,data,"stored");
    }
    public void delete(Context context)throws Exception{
        LearningConsent consent=new LearningConsent(context);consent.revoke();String token=consent.token(false);if(token==null){consent.forgetDeletedToken(null);return;}request(consent.storedOrigin(),"/api/learning/contributions","DELETE",token,null,"deleted");consent.forgetDeletedToken(token);
    }
    private void request(String origin,String path,String method,String token,byte[] body,String confirmation)throws Exception{
        URI url=new URI(origin);if(!"https".equals(url.getScheme())||url.getHost()==null||!url.getRawPath().isEmpty()||url.getRawQuery()!=null||url.getRawFragment()!=null||url.getUserInfo()!=null)throw new IllegalStateException("Secure contribution service unavailable");
        HttpURLConnection connection=(HttpURLConnection)new URL(origin+path).openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(20000);connection.setRequestMethod(method);connection.setRequestProperty("Authorization","Bearer "+token);
        try{if(body!=null){connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json; charset=utf-8");connection.setFixedLengthStreamingMode(body.length);try(var output=connection.getOutputStream()){output.write(body);}}int code=connection.getResponseCode();if(code<200||code>=300)throw new java.io.IOException(code==429?"Contribution limit reached; try later":"Contribution service unavailable; retry manually");try(var input=connection.getInputStream()){String response=new String(BoundedIo.read(input,4096),StandardCharsets.UTF_8);confirmResponse(response,confirmation);}}finally{connection.disconnect();}
    }
    static void confirmResponse(String response,String confirmation)throws java.io.IOException{try{if(!Boolean.TRUE.equals(new org.json.JSONObject(response).opt(confirmation)))throw new java.io.IOException("Submission/deletion was not confirmed");}catch(org.json.JSONException invalid){throw new java.io.IOException("Submission/deletion was not confirmed");}}
}
