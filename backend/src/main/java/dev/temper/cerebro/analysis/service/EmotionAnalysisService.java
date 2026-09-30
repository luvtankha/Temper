package dev.temper.cerebro.analysis.service;
import dev.temper.cerebro.analysis.domain.MessageAnalysis;
import dev.temper.cerebro.chat.domain.Message;
public interface EmotionAnalysisService {MessageAnalysis analyze(Message current);}
