package dev.temper.cerebro.analysis.service;

import dev.temper.cerebro.ai.SentimentModel;
import dev.temper.cerebro.ai.EmotionModel;
import dev.temper.cerebro.ai.SarcasmModel;
import dev.temper.cerebro.ai.ToxicityModel;
import dev.temper.cerebro.analysis.domain.*;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.conversation.context.ContextWindow;
import java.util.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/** Independent configured classifiers replace their fields; unimplemented signals remain explicit fixtures. */
@Primary @Service
public class HybridEmotionAnalysisService implements EmotionAnalysisService {
    private final MockEmotionAnalysisService fixtures;
    private final SentimentModel sentiment;
    private final EmotionModel emotion;
    private final SarcasmModel sarcasm;
    private final ToxicityModel toxicity;
    private final EstimatedLanguageIndicators indicators;
    public HybridEmotionAnalysisService(MockEmotionAnalysisService fixtures, SentimentModel sentiment, EmotionModel emotion, SarcasmModel sarcasm, ToxicityModel toxicity, EstimatedLanguageIndicators indicators) {
        this.fixtures = fixtures; this.sentiment = sentiment; this.emotion=emotion; this.sarcasm=sarcasm; this.toxicity=toxicity; this.indicators=indicators;
    }
    public static String input(ContextWindow context) {
        // Current first preserves its beginning if token truncation occurs. Bound history individually.
        StringBuilder input = new StringBuilder("Current turn (speaker ")
            .append(context.current().speaker().id()).append("): ").append(context.current().text()).append("\nPrevious turns, oldest first:\n");
        for (var turn : context.previous()) input.append(turn.speaker().id()).append(": ")
            .append(turn.text(), 0, Math.min(turn.text().length(), 240)).append('\n');
        return input.toString().replaceAll("(?i)https?://\\S+", "http").replaceAll("(?<!\\w)@\\w+", "@user");
    }
    @Override public MessageAnalysis analyze(Message message, ContextWindow context) {
        return analyze(message,context,input(context),false);
    }
    @Override public MessageAnalysis analyzeVisibleTurn(Message message,ContextWindow context){
        return analyze(message,context,context.current().text().replaceAll("(?i)https?://\\S+","http").replaceAll("(?<!\\w)@\\w+","@user"),true);
    }
    private MessageAnalysis analyze(Message message,ContextWindow context,String modelInput,boolean visibleTurnOnly){
        var baseline = fixtures.analyze(message, context);
        if (!sentiment.available()&&!emotion.available()&&!sarcasm.available()&&!toxicity.available()&&!indicators.available()) return baseline;
        var signals = new LinkedHashMap<>(baseline.signals());
        var emotions=baseline.emotions(); double signed=baseline.sentiment();
        var evidence=new ArrayList<MessageAnalysis.Evidence>();
        if(sentiment.available()) {
            var result=sentiment.classify(modelInput);
            signals.put("negativeSentiment",result.probabilities().get("negative"));
            signed=result.probabilities().get("positive")-result.probabilities().get("negative");
            evidence.add(new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.MODEL,"RoBERTa sentiment",
                result.modelId()+"; probabilities "+result.probabilities()+"; "+result.tokenCount()+" tokens. "+(visibleTurnOnly?"Only the current speaker's text supplied; history remains separate for conversation indicators.":"Current turn and "+context.previous().size()+" preceding speaker-tagged turns supplied;")+" this model was trained on tweets, not multi-turn conversations."));
        }
        if(emotion.available()) {
            var result=emotion.classify(modelInput);emotions=EmotionModel.map(result.probabilities());
            evidence.add(new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.MODEL,"GoEmotions emotion",
                result.modelId()+"; raw independent sigmoid probabilities "+result.probabilities()+". anger→anger, annoyance→frustration proxy, sadness→sadness, joy→happiness, confusion→confusion, max(caring,fear,nervousness)→concern proxy; surprise/neutral retained. "+result.tokenCount()+" tokens "+(visibleTurnOnly?"from the current speaker only; no cross-speaker emotion attribution":"with causal history")+"; Reddit-trained, not dialogue-validated."));
        }
        if(sarcasm.available()) {
            var result=sarcasm.classify(modelInput);signals.put("sarcasm",result.probabilities().get("sarcasm"));
            evidence.add(new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.MODEL,"Sarcasm classifier",
                result.modelId()+"; dedicated probabilities "+result.probabilities()+"; "+result.tokenCount()+" tokens. "+(visibleTurnOnly?"Uses only the current speaker's text":"Uses current text and "+context.previous().size()+" preceding speaker-tagged turns")+", independently of sentiment. Fine-tuning domain is not dialogue-validated; this is an uncertain linguistic estimate."));
        }
        if(toxicity.available()) {
            var result=toxicity.classify(modelInput);var scores=result.probabilities();
            signals.put("toxicity",scores.get("toxic"));signals.put("insult",scores.get("insult"));signals.put("threat",scores.get("threat"));
            signals.put("hostility",Math.max(scores.get("insult"),scores.get("threat")));
            evidence.add(new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.MODEL,"Toxicity classifier",
                result.modelId()+"; raw independent sigmoid probabilities "+scores+"; "+result.tokenCount()+" tokens. Dedicated toxicity model independent of sentiment and sarcasm; supplied "+(visibleTurnOnly?"only the current speaker's text":"current text and causal history")+". Pinned Jigsaw Wikipedia-comment checkpoint, not dialogue-validated; quoted insults and identity terms can cause false positives."));
            evidence.add(new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.HEURISTIC,"Hostility proxy",
                "Estimated hostility = max(insult, threat) from the dedicated model. This explicit engineering proxy is not a separately trained hostility class or a finding about intention."));
        }
        if(indicators.available()){var result=indicators.estimate(context);signals.putAll(result.scores());evidence.addAll(result.evidence());}
        evidence.add(new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.MOCK,"Remaining fixture signals",
            (sentiment.available()?"":"Signed and negative sentiment, ")+(emotion.available()?"":"Emotions, ")+(sarcasm.available()?"":"Sarcasm, ")+(toxicity.available()?"":"Toxicity, ")+(indicators.available()?"":"Passive aggression, blame, defensiveness, ")+"conflict and timeline markers still use ordinal fixtures. See MODEL evidence for the fields replaced by actual inference."));
        return new MessageAnalysis(baseline.messageId(), baseline.speakerId(), baseline.sequence(), AnalysisMode.HYBRID,
            emotions, signals, signed, baseline.conflict(), baseline.contextMessageIds(),
            "Configured local classifiers estimate "+(visibleTurnOnly?"only this speaker's current text; separate inspectable rules retain causal conversation history":"language signals using current text and causal history")+". Source evidence identifies model outputs, semantic proxies and remaining fixtures; these estimates do not establish the speaker’s internal feelings.", evidence, baseline.analyzedAt());
    }
}
