package dev.temper.cerebro.store;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

class StorePurchaseTests {
    private final ObjectMapper json=new ObjectMapper();
    private KeyPair play,server;
    private int checks,acknowledgements;
    private StorePurchaseService.State state;
    private boolean outage;
    @BeforeEach void setup()throws Exception{KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);play=generator.generateKeyPair();server=generator.generateKeyPair();state=new StorePurchaseService.State(0,0,0);}
    private StorePurchaseService service(){return new StorePurchaseService(json,play.getPublic(),server.getPrivate(),new StorePurchaseService.Publisher(){public StorePurchaseService.State get(String product,String token){checks++;return state;}public void acknowledge(String product,String token){if(outage)throw new IllegalStateException();acknowledgements++;}},Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"),ZoneOffset.UTC));}
    private StorePurchaseService.Request request(String packageName,String product,int status)throws Exception{String data=json.writeValueAsString(Map.of("packageName",packageName,"productId",product,"purchaseToken","dummy-token","purchaseState",status));Signature signer=Signature.getInstance("SHA1withRSA");signer.initSign(play.getPrivate());signer.update(data.getBytes(StandardCharsets.UTF_8));return new StorePurchaseService.Request(product,data,Base64.getEncoder().encodeToString(signer.sign()));}
    @Test void verifiesGoogleAcknowledgesAndSignsBoundedEntitlement()throws Exception{var result=service().verify(request(StorePurchaseService.PACKAGE,"temper_avatar_nova",0));assertEquals(1,checks);assertEquals(1,acknowledgements);Signature verifier=Signature.getInstance("SHA256withRSA");verifier.initVerify(server.getPublic());verifier.update(result.payload().getBytes(StandardCharsets.UTF_8));assertTrue(verifier.verify(Base64.getDecoder().decode(result.signature())));var payload=json.readTree(result.payload());assertEquals(86_400_000,payload.get("expiresAt").asLong()-payload.get("issuedAt").asLong());assertFalse(result.payload().contains("dummy-token"));}
    @Test void rejectsTamperedAndWrongPackageBeforeGoogle()throws Exception{var original=request(StorePurchaseService.PACKAGE,"temper_avatar_nova",0);assertThrows(SecurityException.class,()->service().verify(new StorePurchaseService.Request(original.productId(),original.purchaseData().replace("nova","orbit"),original.signature())));assertThrows(SecurityException.class,()->service().verify(request("com.other","temper_avatar_nova",0)));assertEquals(0,checks);}
    @Test void pendingOrCancelledNeverGrants()throws Exception{assertThrows(SecurityException.class,()->service().verify(request(StorePurchaseService.PACKAGE,"temper_avatar_nova",2)));state=new StorePurchaseService.State(1,0,0);assertThrows(SecurityException.class,()->service().verify(request(StorePurchaseService.PACKAGE,"temper_avatar_nova",0)));assertEquals(0,acknowledgements);}
    @Test void refundOrConsumedPurchaseNeverGrants()throws Exception{state=new StorePurchaseService.State(0,1,1);assertThrows(SecurityException.class,()->service().verify(request(StorePurchaseService.PACKAGE,"temper_avatar_nova",0)));assertEquals(0,acknowledgements);}
    @Test void acknowledgementFailureLeavesPurchaseRetryable()throws Exception{outage=true;assertThrows(IllegalStateException.class,()->service().verify(request(StorePurchaseService.PACKAGE,"temper_avatar_nova",0)));outage=false;assertNotNull(service().verify(request(StorePurchaseService.PACKAGE,"temper_avatar_nova",0)));assertEquals(1,acknowledgements);}
    @Test void restoreRevalidatesAndDoesNotConsume()throws Exception{state=new StorePurchaseService.State(0,0,1);var request=request(StorePurchaseService.PACKAGE,"temper_avatar_nova",0);service().verify(request);service().verify(request);assertEquals(2,checks);assertEquals(0,acknowledgements);}
    @Test void malformedReceiptSignatureAndUnknownProductAreRejectedBeforeGoogle(){assertThrows(SecurityException.class,()->service().verify(new StorePurchaseService.Request("temper_avatar_nova","{}","bad")));assertThrows(SecurityException.class,()->service().verify(new StorePurchaseService.Request(null,"{}","bad")));assertThrows(SecurityException.class,()->service().verify(new StorePurchaseService.Request("unknown","{}","bad")));assertEquals(0,checks);}
}
