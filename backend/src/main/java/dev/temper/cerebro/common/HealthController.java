package dev.temper.cerebro.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.temper.cerebro.ai.SentimentModel;
import dev.temper.cerebro.ai.EmotionModel;

@RestController
public class HealthController {
    private final SentimentModel sentiment;
    private final EmotionModel emotion;
    public HealthController(SentimentModel sentiment,EmotionModel emotion) {this.sentiment=sentiment;this.emotion=emotion;}
    public record Health(String status, String application, String analysisMode) {}

    @GetMapping("/api/v1/health")
    public Health health() { return new Health("UP", "TEMPER", sentiment.available()||emotion.available()?"HYBRID":"NONE"); }
}
