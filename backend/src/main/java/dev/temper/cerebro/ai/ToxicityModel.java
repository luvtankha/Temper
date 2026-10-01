package dev.temper.cerebro.ai;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public final class ToxicityModel implements AutoCloseable {
    private final OnnxTextClassifier classifier;
    public ToxicityModel(@Value("${temper.toxicity.model-dir:}") String directory,ObjectMapper json) throws Exception {
        if(directory.isBlank()){classifier=null;return;}
        SentimentModel.Spec spec;
        try(var stream=ToxicityModel.class.getResourceAsStream("/models/toxicity.json")) {
            if(stream==null)throw new IllegalStateException("Missing toxicity specification");
            spec=json.readValue(stream,SentimentModel.Spec.class);
        }
        classifier=new OnnxTextClassifier(Path.of(directory),spec.modelId(),spec.modelSha256(),spec.tokenizerSha256(),spec.labels(),spec.maxTokens(),true);
    }
    public boolean available(){return classifier!=null;}
    public ClassificationResult classify(String text){if(classifier==null)throw new IllegalStateException("Toxicity model unavailable");return classifier.classify(text);}
    @Override public void close() throws Exception {if(classifier!=null)classifier.close();}
}
