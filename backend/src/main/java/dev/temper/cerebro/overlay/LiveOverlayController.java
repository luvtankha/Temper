package dev.temper.cerebro.overlay;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public final class LiveOverlayController {
    private final LiveOverlayService service;
    private final ObjectMapper json;
    private final byte[] authorization;
    private final Semaphore capacity=new Semaphore(1);
    public LiveOverlayController(LiveOverlayService service,ObjectMapper json,@Value("${temper.overlay.token:}") String token){this.service=service;this.json=json;authorization=token.length()>=32?("Bearer "+token).getBytes(StandardCharsets.UTF_8):null;}
    @PostMapping(value="/api/overlay/analyze",produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LiveOverlayService.Response> analyze(HttpServletRequest request){
        String header=request.getHeader("Authorization");if(authorization==null||header==null||header.length()>512||!MessageDigest.isEqual(authorization,header.getBytes(StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Overlay connection unavailable");
        if(!capacity.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Analysis busy");
        byte[] body=null;
        try{
            body=request.getInputStream().readNBytes(65_537);if(body.length>65_536)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Visible window too large");
            LiveOverlayService.Request parsed;
            try{parsed=json.readValue(body,LiveOverlayService.Request.class);LiveOverlayService.validate(parsed);}catch(Exception invalid){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid visible window");}
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.analyze(parsed));
        }catch(ResponseStatusException expected){throw expected;}catch(Exception unavailable){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(LiveOverlayService.unavailable());}finally{if(body!=null)java.util.Arrays.fill(body,(byte)0);capacity.release();}
    }
}
