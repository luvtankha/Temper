package dev.temper.cerebro.analysis.api;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import dev.temper.cerebro.analysis.service.AnalysisService;

@RestController
@RequestMapping("/api/v1")
public class AnalysisController {
    private final AnalysisService service;
    public AnalysisController(AnalysisService service) {this.service=service;}
    @PostMapping("/messages/{id}/analyze") public AnalysisService.Turn analyze(@PathVariable UUID id) {return service.analyze(id);}
    @GetMapping("/messages/{id}/analysis") public AnalysisService.Turn get(@PathVariable UUID id) {return service.get(id);}
    @GetMapping("/conversations/{id}/analytics") public AnalysisService.Snapshot analytics(@PathVariable UUID id) {return service.snapshot(id);}
    @GetMapping("/conversations/{id}/timeline") public Map<String,Object> timeline(@PathVariable UUID id) {var s=service.snapshot(id);return Map.of("conversationId",id,"mode",s.mode(),"messages",s.messages(),"events",s.events(),"pendingMessages",s.pendingMessages());}
    @GetMapping("/conversations/{id}/insights") public Map<String,Object> insights(@PathVariable UUID id) {var s=service.snapshot(id);return Map.of("conversationId",id,"mode",s.mode(),"events",s.events(),"explanation","Ordinal fixtures only. No language inference or real conflict engine has run.","pendingMessages",s.pendingMessages());}
}
