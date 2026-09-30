package dev.temper.cerebro;

import dev.temper.cerebro.ai.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "TEMPER_FOUNDATION_MODEL_DIR", matches = ".+")
class FoundationInferenceTest {
    @Test void genuineNativeInferenceUsesTokensAndReusesSession() throws Exception {
        try (var classifier = new OnnxTextClassifier(Path.of(System.getenv("TEMPER_FOUNDATION_MODEL_DIR")))) {
            var positive = classifier.classify("I loved this wonderful movie. It was fantastic!");
            var negative = classifier.classify("I hated this awful movie. It was terrible!");
            assertTrue(positive.probabilities().get("positive") > .9);
            assertTrue(negative.probabilities().get("negative") > .9);
            assertEquals(1, positive.probabilities().values().stream().mapToDouble(Double::doubleValue).sum(), 1e-6);
            assertEquals(positive.probabilities(), classifier.classify("I loved this wonderful movie. It was fantastic!").probabilities());
            assertTrue(classifier.classify("movie ".repeat(1000)).tokenCount() <= 128);
            assertThrows(IllegalArgumentException.class, () -> classifier.classify(" "));
            assertEquals("MODEL", positive.source());
        }
    }
}
