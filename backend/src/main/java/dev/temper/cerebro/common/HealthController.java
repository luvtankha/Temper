package dev.temper.cerebro.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.temper.cerebro.ai.SentimentModel;

@RestController
public class HealthController {
    private final SentimentModel sentiment;
    public HealthController(SentimentModel sentiment) {this.sentiment=sentiment;}
    public record Health(String status, String application, String analysisMode) {}

    @GetMapping("/api/v1/health")
    public Health health() { return new Health("UP", "TEMPER", sentiment.available()?"HYBRID":"NONE"); }
}
