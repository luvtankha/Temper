package dev.temper.cerebro.ai;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public final class SarcasmModel implements AutoCloseable {
    private final OnnxTextClassifier classifier;
    public SarcasmModel(@Value("${temper.sarcasm.model-dir:}") String directory,ObjectMapper json) throws Exception {
        if(directory.isBlank()){classifier=null;return;}
        SentimentModel.Spec spec;
        try(var stream=SarcasmModel.class.getResourceAsStream("/models/sarcasm.json")) {
            if(stream==null)throw new IllegalStateException("Missing sarcasm specification");
            spec=json.readValue(stream,SentimentModel.Spec.class);
        }
        classifier=new OnnxTextClassifier(Path.of(directory),spec.modelId(),spec.modelSha256(),spec.tokenizerSha256(),spec.labels(),spec.maxTokens());
    }
    public boolean available(){return classifier!=null;}
    public ClassificationResult classify(String text){if(classifier==null)throw new IllegalStateException("Sarcasm model unavailable");return classifier.classify(text);}
    @Override public void close() throws Exception {if(classifier!=null)classifier.close();}
}
