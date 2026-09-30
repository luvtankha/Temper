package dev.temper.cerebro.ai;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Configuration
public class FoundationConfiguration {
    @Bean(destroyMethod = "")
    TextClassifier foundationClassifier(@Value("${temper.foundation.model-dir:}") String directory) throws Exception {
        if (directory.isBlank()) return text -> { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "Local foundation classifier is not configured."); };
        return new OnnxTextClassifier(Path.of(directory));
    }
    // Explicit destruction supports an optional classifier while retaining native lifecycle cleanup.
    @Bean
    AutoCloseable foundationLifecycle(TextClassifier foundationClassifier) {
        return () -> { if (foundationClassifier instanceof AutoCloseable resource) resource.close(); };
    }
}
