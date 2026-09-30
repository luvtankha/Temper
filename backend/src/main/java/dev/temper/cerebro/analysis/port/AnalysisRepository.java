package dev.temper.cerebro.analysis.port;
import java.util.*;
import dev.temper.cerebro.analysis.domain.*;
public interface AnalysisRepository {
    Optional<MessageAnalysis> findMessageAnalysis(UUID messageId); void saveMessageAnalysis(MessageAnalysis analysis);
    Optional<ConversationAnalysis> findConversationAnalysis(UUID conversationId); void saveConversationAnalysis(ConversationAnalysis analysis);
}
