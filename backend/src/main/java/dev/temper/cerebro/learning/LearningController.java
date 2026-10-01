package dev.temper.cerebro.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;

@RestController
@ConditionalOnProperty(name="temper.learning.enabled",havingValue="true")
public final class LearningController {
    private final EncryptedLearningStore store;private final ObjectMapper mapper=new ObjectMapper();
    @org.springframework.beans.factory.annotation.Autowired
    public LearningController(@Value("${temper.learning.directory}")String directory,@Value("${temper.learning.key-file}")String keyFile,@Value("${temper.store.enabled:false}")boolean storeEnabled)throws Exception{
        if(storeEnabled||directory.isBlank()||keyFile.isBlank())throw new IllegalArgumentException("Deploy learning separately with private storage and a key file");
        Path file=Path.of(keyFile);if(Files.size(file)>100)throw new IllegalArgumentException("Invalid storage key file");store=new EncryptedLearningStore(Path.of(directory),Base64.getDecoder().decode(Files.readString(file).strip()),Clock.systemUTC());
    }
    public LearningController(EncryptedLearningStore store){this.store=store;}
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=3_600_000)
    public void expire(){try{store.purge();}catch(Exception unavailable){/* No record data is logged. Operator must monitor private volume health. */}}
    private static String token(String authorization){if(authorization==null||!authorization.startsWith("Bearer "))throw new SecurityException();return authorization.substring(7);}
    @PostMapping("/api/learning/sessions")
    public ResponseEntity<?> submit(@RequestHeader(value="Authorization",required=false)String authorization,@RequestBody byte[] body){
        try{String token=token(authorization);EncryptedLearningStore.contributor(token);LearningSubmission submission=LearningSubmission.parse(mapper.readTree(body));boolean created=store.save(token,submission);return ResponseEntity.status(created?201:200).body(Map.of("stored",true,"retentionDays",EncryptedLearningStore.RETENTION_DAYS));}
        catch(SecurityException error){return ResponseEntity.status(401).body(Map.of("error","Contribution authorization required"));}
        catch(IllegalArgumentException|com.fasterxml.jackson.core.JsonProcessingException error){return ResponseEntity.badRequest().body(Map.of("error","Invalid consent, review or bounded session"));}
        catch(IllegalStateException error){return ResponseEntity.status(429).body(Map.of("error","Contribution limit reached"));}
        catch(Exception error){return ResponseEntity.status(503).body(Map.of("error","Contribution service unavailable"));}
    }
    @DeleteMapping("/api/learning/contributions")
    public ResponseEntity<?> delete(@RequestHeader(value="Authorization",required=false)String authorization){try{store.delete(token(authorization));return ResponseEntity.ok(Map.of("deleted",true));}catch(SecurityException error){return ResponseEntity.status(401).body(Map.of("error","Contribution authorization required"));}catch(Exception error){return ResponseEntity.status(503).body(Map.of("error","Deletion service unavailable; retry"));}}
}
