package dev.temper.cerebro.analysis.service;
import dev.temper.cerebro.analysis.domain.MessageAnalysis;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.conversation.context.ContextWindow;
public interface EmotionAnalysisService {
    MessageAnalysis analyze(Message current,ContextWindow context);
    /** Single-speaker classifiers must not attribute other participants' text to this turn. */
    default MessageAnalysis analyzeVisibleTurn(Message current,ContextWindow context){return analyze(current,context);}
}
