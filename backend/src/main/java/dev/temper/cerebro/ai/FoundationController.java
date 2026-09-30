package dev.temper.cerebro.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/foundation")
public class FoundationController {
    private final TextClassifier classifier;
    public FoundationController(TextClassifier classifier) { this.classifier = classifier; }
    public record Request(@NotBlank @Size(max = 12000) String text) {}
    @PostMapping("/classify")
    public ClassificationResult classify(@Valid @RequestBody Request request) { return classifier.classify(request.text()); }
}
