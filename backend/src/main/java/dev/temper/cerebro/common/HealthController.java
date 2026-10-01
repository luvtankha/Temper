package dev.temper.cerebro.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.temper.cerebro.ai.SentimentModel;
import dev.temper.cerebro.ai.EmotionModel;
import dev.temper.cerebro.ai.SarcasmModel;
import dev.temper.cerebro.ai.ToxicityModel;

@RestController
public class HealthController {
    private final SentimentModel sentiment;
    private final EmotionModel emotion;
    private final SarcasmModel sarcasm;
    private final ToxicityModel toxicity;
    public HealthController(SentimentModel sentiment,EmotionModel emotion,SarcasmModel sarcasm,ToxicityModel toxicity) {this.sentiment=sentiment;this.emotion=emotion;this.sarcasm=sarcasm;this.toxicity=toxicity;}
    public record Health(String status, String application, String analysisMode) {}

    @GetMapping("/api/v1/health")
    public Health health() { return new Health("UP", "TEMPER", sentiment.available()||emotion.available()||sarcasm.available()||toxicity.available()?"HYBRID":"NONE"); }
}
