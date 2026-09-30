package dev.temper.cerebro.analysis.service;
import dev.temper.cerebro.analysis.domain.MessageAnalysis;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.conversation.context.ContextWindow;
public interface EmotionAnalysisService {MessageAnalysis analyze(Message current,ContextWindow context);}
