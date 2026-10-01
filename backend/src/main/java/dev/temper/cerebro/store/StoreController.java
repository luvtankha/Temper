package dev.temper.cerebro.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnProperty(name="temper.store.enabled",havingValue="true")
public final class StoreController {
    private final StorePurchaseService service;
    public StoreController(ObjectMapper json,@Value("${temper.store.play-key}")String playKey,@Value("${temper.store.signing-key}")String signingKey,@Value("${temper.store.credentials}")String credentials)throws Exception{
        Path key=Path.of(signingKey);if(Files.size(key)>65536)throw new IllegalArgumentException("Invalid store signing key");
        service=new StorePurchaseService(json,KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(playKey))),GooglePlayPublisher.privateKey(Files.readString(key)),new GooglePlayPublisher(Path.of(credentials),json),Clock.systemUTC());
    }
    @PostMapping(value="/api/store/verify",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verify(@RequestBody StorePurchaseService.Request request,HttpServletRequest incoming){
        if(incoming.getContentLengthLong()>16384)return ResponseEntity.status(413).build();
        try{return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.verify(request));}
        catch(SecurityException invalid){return ResponseEntity.status(403).build();}
        catch(Exception unavailable){return ResponseEntity.status(503).build();}
    }
}
