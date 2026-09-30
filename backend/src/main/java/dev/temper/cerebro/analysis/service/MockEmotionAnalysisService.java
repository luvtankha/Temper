package dev.temper.cerebro.analysis.service;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import dev.temper.cerebro.analysis.domain.*;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.conversation.context.ContextWindow;

/** Ordinal fixtures ONLY. No inspection of message text or inferred feelings. */
@Service
public class MockEmotionAnalysisService implements EmotionAnalysisService {
    private final Clock clock;
    public MockEmotionAnalysisService(Clock clock) {this.clock=clock;}
    private static final String[] EMOTIONS={"frustration","anger","sadness","happiness","confusion","concern"};
    private static final String[] SIGNALS={"negativeSentiment","sarcasm","toxicity","passiveAggression","defensiveness","blame"};
    private static final double[][] E={{.05,.02,.02,.38,.08,.12},{.02,.01,.01,.82,.02,.05},{.08,.02,.03,.51,.07,.2},{.38,.13,.12,.08,.21,.63},{.64,.32,.22,.04,.48,.51},{.41,.18,.12,.11,.18,.48},{.16,.06,.05,.48,.1,.25}};
    private static final double[][] S={{.04,.02,.01,.03,.04,.02},{.02,.01,.01,.02,.02,.01},{.08,.02,.01,.05,.08,.03},{.56,.09,.02,.18,.34,.14},{.73,.22,.06,.36,.51,.42},{.39,.08,.02,.14,.43,.21},{.13,.03,.01,.04,.12,.05}};
    private static final double[] SENTIMENT={.45,.86,.38,-.46,-.63,-.25,.41},CONFLICT={.06,.03,.12,.39,.58,.34,.13};
    public MessageAnalysis analyze(Message message,ContextWindow context) {
        if(!context.current().messageId().equals(message.id())||!context.conversationId().equals(message.conversationId()))throw new IllegalArgumentException("Analysis context must match current turn");
        int index=(int)((message.sequence()-1)%7);
        return new MessageAnalysis(message.id(),message.speakerId(),message.sequence(),AnalysisMode.MOCK,map(EMOTIONS,E[index]),map(SIGNALS,S[index]),SENTIMENT[index],CONFLICT[index],context.previous().stream().map(ContextWindow.Turn::messageId).toList(),
            "Hand-authored ordinal fixture. Text has not been classified; these scores do not establish the speaker’s internal feelings.",
            List.of(new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.MOCK,"Ordinal fixture "+(index+1),"Scores are assigned by message sequence only. No AI inference or contextual interpretation has run."),new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.MOCK,"Context available",context.previous().size()+" preceding turns and speaker identities were supplied. The mock does not interpret their text.")),clock.instant());
    }
    private static Map<String,Double> map(String[] keys,double[] values) {Map<String,Double> result=new LinkedHashMap<>();for(int i=0;i<keys.length;i++)result.put(keys[i],values[i]);return result;}
}
