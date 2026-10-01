package dev.temper.cerebro.store;

import com.fasterxml.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

/** Only signed receipts independently confirmed by Google can issue a cosmetic entitlement. */
public final class StorePurchaseService {
    public static final Set<String> PRODUCTS=Set.of("temper_avatar_nova","temper_avatar_orbit","temper_avatar_luma");
    public static final String PACKAGE="dev.temper.android";
    public record Request(String productId,String purchaseData,String signature){@Override public String toString(){return "Purchase verification [redacted]";}}
    public record Envelope(String payload,String signature){}
    public record State(int purchaseState,int consumptionState,int acknowledgementState){}
    public interface Publisher {State get(String product,String token)throws Exception;void acknowledge(String product,String token)throws Exception;}
    private final ObjectMapper json;
    private final PublicKey playKey;
    private final PrivateKey entitlementKey;
    private final Publisher publisher;
    private final Clock clock;
    public StorePurchaseService(ObjectMapper json,PublicKey playKey,PrivateKey entitlementKey,Publisher publisher,Clock clock){this.json=json;this.playKey=playKey;this.entitlementKey=entitlementKey;this.publisher=publisher;this.clock=clock;}
    public synchronized Envelope verify(Request request)throws Exception{
        if(request==null||request.productId()==null||!PRODUCTS.contains(request.productId())||request.purchaseData()==null||request.purchaseData().length()>8192||request.signature()==null||request.signature().length()>2048)throw new SecurityException();
        Signature receipt=Signature.getInstance("SHA1withRSA");receipt.initVerify(playKey);receipt.update(request.purchaseData().getBytes(StandardCharsets.UTF_8));
        try{if(!receipt.verify(Base64.getDecoder().decode(request.signature())))throw new SecurityException();}catch(IllegalArgumentException|SignatureException invalid){throw new SecurityException();}
        JsonNode data=json.readTree(request.purchaseData());String token=data.path("purchaseToken").asText(data.path("token").asText());
        if(!PACKAGE.equals(data.path("packageName").asText())||!request.productId().equals(data.path("productId").asText())||data.path("purchaseState").asInt(-1)!=0||token.isBlank()||token.length()>2048)throw new SecurityException();
        State state=publisher.get(request.productId(),token);if(state.purchaseState()!=0||state.consumptionState()!=0)throw new SecurityException();
        long issued=clock.millis();String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        String payload=json.writeValueAsString(Map.of("packageName",PACKAGE,"productId",request.productId(),"tokenHash",hash,"issuedAt",issued,"expiresAt",issued+86_400_000));
        Signature signer=Signature.getInstance("SHA256withRSA");signer.initSign(entitlementKey);signer.update(payload.getBytes(StandardCharsets.UTF_8));Envelope entitlement=new Envelope(payload,Base64.getEncoder().encodeToString(signer.sign()));
        if(state.acknowledgementState()!=1)publisher.acknowledge(request.productId(),token);
        return entitlement;
    }
}
