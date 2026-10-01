package dev.temper.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONObject;
import dev.temper.android.store.EntitlementVerifier;
import dev.temper.android.overlay.FloatingPlacement;
import dev.temper.android.inference.EmotionEstimate;
import dev.temper.android.character.Avatar;

public class ConsumerTests {
    private String envelope(String product,long issued,long expires,KeyPair key)throws Exception{
        String data=new JSONObject().put("packageName","dev.temper.android").put("productId",product).put("tokenHash","a".repeat(64)).put("issuedAt",issued).put("expiresAt",expires).toString();Signature signer=Signature.getInstance("SHA256withRSA");signer.initSign(key.getPrivate());signer.update(data.getBytes(StandardCharsets.UTF_8));return new JSONObject().put("payload",data).put("signature",Base64.getEncoder().encodeToString(signer.sign())).toString();
    }
    @Test public void ownershipRequiresValidSignedCurrentProduct()throws Exception{
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KeyPair key=generator.generateKeyPair();String publicKey=Base64.getEncoder().encodeToString(key.getPublic().getEncoded());long now=1_700_000_000_000L;
        String valid=envelope("temper_avatar_nova",now,now+86_400_000,key);assertTrue(EntitlementVerifier.valid(valid,publicKey,"temper_avatar_nova",now));
        assertFalse(EntitlementVerifier.valid(valid.replace("nova","orbit"),publicKey,"temper_avatar_orbit",now));assertFalse(EntitlementVerifier.valid(valid,publicKey,"temper_avatar_orbit",now));assertFalse(EntitlementVerifier.valid(valid,publicKey,"temper_avatar_nova",now+86_400_001));assertFalse(EntitlementVerifier.valid(valid,"","temper_avatar_nova",now));
        assertFalse(EntitlementVerifier.valid(envelope("temper_avatar_nova",now+600_000,now+86_400_000,key),publicKey,"temper_avatar_nova",now));assertFalse(EntitlementVerifier.valid(envelope("temper_avatar_nova",now,now+172_800_000,key),publicKey,"temper_avatar_nova",now));
    }
    @Test public void draggedCharacterCannotEscapeDisplay(){
        for(int width:new int[]{320,1080,2400})for(int height:new int[]{480,1080,2400})for(float x:new float[]{-2,0,.5f,1,2})for(float y:new float[]{-2,0,.5f,1,2}){var p=FloatingPlacement.place(x,y,width,height,64,88,32,32);assertTrue(p.x()>=0&&p.x()+64<=width);assertTrue(p.y()>=32&&p.y()+88<=height-32);}
    }
    @Test public void reassuringCaringDoesNotBecomeDistress(){float[] logits=new float[28];Arrays.fill(logits,-10);logits[5]=10;logits[27]=3;float[] spectrum=EmotionEstimate.spectrum(logits);assertTrue(spectrum[2]<.001f);assertEquals("UNCERTAIN",EmotionEstimate.summarize(spectrum,null).trajectory());assertTrue(EmotionEstimate.summarize(spectrum,null).summary().available());}
    @Test public void fallingAndRisingTensionUseSameSpeakerScores(){float[] neutral={.9f,.01f,.02f,.01f,.01f,.02f,.02f,.01f},angry={.01f,.01f,.04f,.01f,.01f,.7f,.95f,.01f};assertEquals("TENSION_RISING",EmotionEstimate.summarize(angry,neutral).trajectory());assertEquals("DEESCALATING",EmotionEstimate.summarize(neutral,angry).trajectory());assertEquals("STABLE",EmotionEstimate.summarize(neutral,neutral).trajectory());assertEquals("ESCALATION_RISK",EmotionEstimate.summarize(angry,angry).trajectory());}
    @Test public void catalogHasOneFreeStarterAndUniquePlayProducts(){assertEquals(1,Arrays.stream(Avatar.values()).filter(Avatar::free).count());Set<String> products=new HashSet<>();for(Avatar avatar:Avatar.values())if(!avatar.free()){assertTrue(products.add(avatar.productId()));assertSame(avatar,Avatar.fromProduct(avatar.productId()).orElseThrow());}assertSame(Avatar.ALEX,Avatar.fromId("unknown"));}
}
