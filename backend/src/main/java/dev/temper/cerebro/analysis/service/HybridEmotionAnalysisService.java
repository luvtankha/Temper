package dev.temper.cerebro.analysis.service;

import dev.temper.cerebro.ai.SentimentModel;
import dev.temper.cerebro.analysis.domain.*;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.conversation.context.ContextWindow;
import java.util.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/** Replaces only sentiment; unimplemented signals remain explicitly identified fixtures. */
@Primary @Service
public class HybridEmotionAnalysisService implements EmotionAnalysisService {
    private final MockEmotionAnalysisService fixtures;
    private final SentimentModel sentiment;
    public HybridEmotionAnalysisService(MockEmotionAnalysisService fixtures, SentimentModel sentiment) {
        this.fixtures = fixtures; this.sentiment = sentiment;
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
        var baseline = fixtures.analyze(message, context);
        if (!sentiment.available()) return baseline;
        var result = sentiment.classify(input(context));
        var signals = new LinkedHashMap<>(baseline.signals());
        signals.put("negativeSentiment", result.probabilities().get("negative"));
        double signed = result.probabilities().get("positive") - result.probabilities().get("negative");
        var evidence = List.of(
            new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.MODEL, "RoBERTa sentiment",
                result.modelId() + "; probabilities " + result.probabilities() + "; " + result.tokenCount() + " tokens. Current turn and " + context.previous().size() + " preceding speaker-tagged turns supplied; this model was trained on tweets, not multi-turn conversations."),
            new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.MOCK, "Remaining fixture signals",
                "Emotion, sarcasm, toxicity, passive aggression, blame, defensiveness, conflict and timeline markers still use ordinal fixtures. Only signed sentiment and negativeSentiment use actual model inference."));
        return new MessageAnalysis(baseline.messageId(), baseline.speakerId(), baseline.sequence(), AnalysisMode.HYBRID,
            baseline.emotions(), signals, signed, baseline.conflict(), baseline.contextMessageIds(),
            "Sentiment is estimated by local RoBERTa using current text and causal history. Other scores remain hand-authored fixtures; these estimates do not establish the speaker’s internal feelings.", evidence, baseline.analyzedAt());
    }
}
