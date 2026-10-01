package dev.temper.android.store;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.json.JSONObject;
import dev.temper.android.character.Avatar;

/** Server-signed, short lived ownership. A preference or pending payment cannot unlock an avatar. */
public final class EntitlementVerifier {
    private EntitlementVerifier(){}
    public static boolean valid(String envelope,String publicKey,String product,long now){
        try{
            if(envelope==null||envelope.length()>8192||publicKey.isBlank()||Avatar.fromProduct(product).isEmpty())return false;
            JSONObject outer=new JSONObject(envelope);String payload=outer.getString("payload");
            Signature signature=Signature.getInstance("SHA256withRSA");
            signature.initVerify(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))));
            signature.update(payload.getBytes(StandardCharsets.UTF_8));if(!signature.verify(Base64.getDecoder().decode(outer.getString("signature"))))return false;
            JSONObject claim=new JSONObject(payload);long issued=claim.getLong("issuedAt"),expires=claim.getLong("expiresAt");
            return "dev.temper.android".equals(claim.getString("packageName"))&&product.equals(claim.getString("productId"))
                &&claim.getString("tokenHash").matches("[a-f0-9]{64}")&&issued<=now+300_000&&expires>now&&expires>issued&&expires-issued<=86_400_000;
        }catch(Exception invalid){return false;}
    }
}
