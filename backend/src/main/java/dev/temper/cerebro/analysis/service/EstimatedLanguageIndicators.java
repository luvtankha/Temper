package dev.temper.cerebro.analysis.service;
import dev.temper.cerebro.conversation.context.ContextWindow;
import dev.temper.cerebro.analysis.domain.MessageAnalysis;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Small English lexical estimates, not intent classifiers. Rules and matches remain inspectable. */
@Service
public final class EstimatedLanguageIndicators {
    private final boolean enabled;
    public EstimatedLanguageIndicators(@Value("${temper.indicators.enabled:true}") boolean enabled){this.enabled=enabled;}
    public boolean available(){return enabled;}
    private static final Map<String,List<String>> CUES=Map.of(
        "passiveAggression",List.of("whatever you say","if you say so","must be nice","fine, do whatever","thanks for nothing"),
        "blame",List.of("your fault","you caused","you always","you never","because of you","you are to blame"),
        "defensiveness",List.of("not my fault","i did nothing wrong","don't blame me","do not blame me","i was only trying","i'm just saying"),
        "disagreement",List.of("i disagree","i don't agree","i do not agree","that's not true","that is not true","you are wrong","you're wrong","no, that's wrong"),
        "withdrawal",List.of("i'm done talking","i am done talking","leave me alone","i won't discuss","i will not discuss","end of discussion","i don't want to talk","i do not want to talk"));
    private static final List<String> ORDER=List.of("passiveAggression","blame","defensiveness","disagreement","withdrawal");
    public record Result(Map<String,Double> scores,List<MessageAnalysis.Evidence> evidence){}
    private static String normalized(String text){
        return text.toLowerCase(Locale.ROOT).replace('\u2019','\'').replaceAll("\"[^\"]*\"|“[^”]*”", " ").replaceAll("\\s+"," ");
    }
    private static List<String> matches(String text,String key){
        String value=normalized(text);
        return CUES.get(key).stream().filter(cue->Pattern.compile("(?<![a-z])"+Pattern.quote(cue)+"(?![a-z])").matcher(value).find())
            .filter(cue->!value.contains("not "+cue)&&!value.contains("isn't "+cue)).toList();
    }
    public Result estimate(ContextWindow context){
        var scores=new LinkedHashMap<String,Double>();var evidence=new ArrayList<MessageAnalysis.Evidence>();
        for(String key:ORDER){
            var cues=matches(context.current().text(),key);double score=cues.isEmpty()?0:Math.min(.85,.55+.15*(cues.size()-1));scores.put(key,score);
            evidence.add(new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.HEURISTIC,"Estimated "+key,
                "English lexical indicator v1; matched cues "+cues+". Score: no recognized cue=0, otherwise min(0.85,0.55+0.15*(distinct cues-1)). Double-quoted spans and direct negation of a cue are excluded; broader negation, irony and context remain uncertain. This is an estimated indicator, not a probability or finding of intention."));
        }
        var prior=context.previous().stream().filter(t->t.speaker().id().equals(context.current().speaker().id()))
            .filter(t->!matches(t.text(),"disagreement").isEmpty()).toList();
        double repeated=scores.get("disagreement")>0&&!prior.isEmpty()?Math.min(.85,.55+.15*(prior.size()-1)):0;
        scores.put("repeatedDisagreement",repeated);
        evidence.add(new MessageAnalysis.Evidence(MessageAnalysis.EvidenceSource.HEURISTIC,"Estimated repeatedDisagreement",
            "Current disagreement cue plus earlier disagreement cues from the same speaker in the bounded causal context. Earlier matching sequences: "+prior.stream().map(ContextWindow.Turn::sequence).toList()+". Score=0 unless both present; otherwise min(0.85,0.55+0.15*(matching prior turns-1)). Estimated repetition, not evidence about motivation."));
        return new Result(Collections.unmodifiableMap(scores),List.copyOf(evidence));
    }
}
