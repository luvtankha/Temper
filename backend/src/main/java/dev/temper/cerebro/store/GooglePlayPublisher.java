package dev.temper.cerebro.store;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.*;

/** Fixed Google endpoints. Service-account credentials never leave this server except signed OAuth JWTs. */
public final class GooglePlayPublisher implements StorePurchaseService.Publisher {
    private final ObjectMapper json;
    private final String email;
    private final PrivateKey key;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
    private String accessToken;
    private long expiry;
    public GooglePlayPublisher(Path credentials,ObjectMapper json)throws Exception{
        this.json=json;if(Files.size(credentials)>65536)throw new IllegalArgumentException("Invalid store credentials");JsonNode config=json.readTree(Files.readString(credentials));email=config.path("client_email").asText();if(!email.endsWith(".iam.gserviceaccount.com"))throw new IllegalArgumentException("Invalid store credentials");key=privateKey(config.path("private_key").asText());
    }
    public static PrivateKey privateKey(String pem)throws Exception{return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(pem.replace("-----BEGIN PRIVATE KEY-----","").replace("-----END PRIVATE KEY-----",""))));}
    private static String url64(byte[] value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value);}
    private synchronized String token()throws Exception{
        long now=System.currentTimeMillis()/1000;if(accessToken!=null&&expiry>now+60)return accessToken;
        String unsigned=url64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))+"."+url64(json.writeValueAsBytes(Map.of("iss",email,"scope","https://www.googleapis.com/auth/androidpublisher","aud","https://oauth2.googleapis.com/token","iat",now,"exp",now+3600)));
        Signature signer=Signature.getInstance("SHA256withRSA");signer.initSign(key);signer.update(unsigned.getBytes(StandardCharsets.UTF_8));String assertion=unsigned+"."+url64(signer.sign());
        String body="grant_type="+URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer",StandardCharsets.UTF_8)+"&assertion="+URLEncoder.encode(assertion,StandardCharsets.UTF_8);
        HttpResponse<String> response=http.send(HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token")).timeout(Duration.ofSeconds(15)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200)throw new IllegalStateException("Store authorization unavailable");JsonNode result=json.readTree(response.body());accessToken=result.path("access_token").asText();if(accessToken.isBlank())throw new IllegalStateException("Store authorization unavailable");expiry=now+Math.min(3600,result.path("expires_in").asInt(300));return accessToken;
    }
    private static URI uri(String product,String token,String suffix){return URI.create("https://androidpublisher.googleapis.com/androidpublisher/v3/applications/"+StorePurchaseService.PACKAGE+"/purchases/products/"+URLEncoder.encode(product,StandardCharsets.UTF_8)+"/tokens/"+URLEncoder.encode(token,StandardCharsets.UTF_8)+suffix);}
    @Override public StorePurchaseService.State get(String product,String purchaseToken)throws Exception{
        HttpResponse<String> response=http.send(HttpRequest.newBuilder(uri(product,purchaseToken,"")).timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+token()).GET().build(),HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()==400||response.statusCode()==404||response.statusCode()==410)throw new SecurityException();if(response.statusCode()!=200)throw new IllegalStateException("Store verification unavailable");JsonNode data=json.readTree(response.body());return new StorePurchaseService.State(data.path("purchaseState").asInt(-1),data.path("consumptionState").asInt(-1),data.path("acknowledgementState").asInt(-1));
    }
    @Override public void acknowledge(String product,String purchaseToken)throws Exception{
        HttpResponse<String> response=http.send(HttpRequest.newBuilder(uri(product,purchaseToken,":acknowledge")).timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+token()).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),HttpResponse.BodyHandlers.ofString());if(response.statusCode()!=200&&response.statusCode()!=204)throw new IllegalStateException("Purchase acknowledgement unavailable");
    }
}
