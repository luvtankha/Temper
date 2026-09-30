package dev.temper.cerebro.common;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

/** Stable public errors never include submitted conversation text. */
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class,MethodArgumentTypeMismatchException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String,String>> badRequest(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("code","INVALID_REQUEST","message","Check the request fields, participant membership and identifier format."));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("code",error.getStatusCode().value()==404?"NOT_FOUND":"ANALYSIS_UNAVAILABLE","message",error.getReason()==null?"Request unavailable":error.getReason()));
    }
}
