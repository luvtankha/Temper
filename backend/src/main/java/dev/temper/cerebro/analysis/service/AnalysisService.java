package dev.temper.cerebro.analysis.service;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import dev.temper.cerebro.analysis.domain.*;
import dev.temper.cerebro.analysis.port.AnalysisRepository;
import dev.temper.cerebro.chat.domain.Message;
import dev.temper.cerebro.chat.port.MessageRepository;
import dev.temper.cerebro.conversation.service.ConversationService;

@Service
public class AnalysisService {
    private final MessageRepository messages;private final AnalysisRepository analyses;
    private final EmotionAnalysisService engine;private final ConversationService conversations;
    public AnalysisService(MessageRepository messages,AnalysisRepository analyses,EmotionAnalysisService engine,ConversationService conversations) {this.messages=messages;this.analyses=analyses;this.engine=engine;this.conversations=conversations;}
    public Message message(UUID id) {return messages.findMessage(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Message not found"));}
    public record Turn(UUID messageId,String speakerId,long sequence,Instant sentAt,AnalysisMode mode,Map<String,Double> emotions,Map<String,Double> signals,double sentiment,double conflict,List<UUID> contextMessageIds,String explanation,List<MessageAnalysis.Evidence> evidence) {}
    private Turn view(MessageAnalysis a) {return new Turn(a.messageId(),a.speakerId(),a.sequence(),message(a.messageId()).sentAt(),a.mode(),a.emotions(),a.signals(),a.sentiment(),a.conflict(),a.contextMessageIds(),a.explanation(),a.evidence());}
    public Turn analyze(UUID id) {var result=engine.analyze(message(id));analyses.saveMessageAnalysis(result);return view(result);}
    public Turn get(UUID id) {message(id);return view(analyses.findMessageAnalysis(id).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Analysis has not been requested for this message")));}
    public record Event(long sequence,UUID messageId,String kind,String label) {}
    public record Snapshot(UUID conversationId,AnalysisMode mode,List<Turn> messages,Double conflictScore,Double sentiment,Double emotionalIntensity,String direction,Long escalationStart,Long peakTension,List<Event> events,long pendingMessages) {}
    public Snapshot snapshot(UUID id) {
        var all=conversations.messages(id);
        var turns=all.stream().map(m->analyses.findMessageAnalysis(m.id())).flatMap(Optional::stream).map(this::view).toList();
        if(turns.isEmpty())return new Snapshot(id,AnalysisMode.MOCK,List.of(),null,null,null,"steady",null,null,List.of(),all.size());
        var last=turns.getLast();var previous=turns.size()>1?turns.get(turns.size()-2):last;
        var peak=turns.stream().max(Comparator.comparingDouble(Turn::conflict)).orElseThrow();
        var start=turns.stream().filter(t->t.conflict()>=.35).findFirst();
        var recovery=turns.stream().filter(t->t.sequence()==7).findFirst();
        List<Event> events=new ArrayList<>();
        start.ifPresent(t->events.add(new Event(t.sequence(),t.messageId(),"escalation","Fixture escalation / major shift")));
        events.add(new Event(peak.sequence(),peak.messageId(),"peak","Fixture peak tension"));
        recovery.ifPresent(t->events.add(new Event(t.sequence(),t.messageId(),"recovery","Fixture recovery")));
        String direction=Math.abs(last.conflict()-previous.conflict())<.08?"steady":last.conflict()>previous.conflict()?"escalating":"recovering";
        return new Snapshot(id,AnalysisMode.MOCK,turns,last.conflict(),last.sentiment(),last.emotions().values().stream().mapToDouble(Double::doubleValue).max().orElse(0),direction,start.map(Turn::sequence).orElse(null),peak.sequence(),List.copyOf(events),all.size()-turns.size());
    }
}
