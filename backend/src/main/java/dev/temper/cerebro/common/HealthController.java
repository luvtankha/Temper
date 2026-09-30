package dev.temper.cerebro.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    public record Health(String status, String application, String analysisMode) {}

    @GetMapping("/api/v1/health")
    public Health health() { return new Health("UP", "TEMPER", "NONE"); }
}
