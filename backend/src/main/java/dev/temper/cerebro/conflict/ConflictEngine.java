package dev.temper.cerebro.conflict;
import dev.temper.cerebro.analysis.domain.MessageAnalysis;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Source-aware engineering aggregation; neither calibrated conflict probability nor intent detection. */
@Service
public final class ConflictEngine {
    public enum Trend { UP, FLAT, DOWN }
    public record Contributor(String signal,double value,double weight,double contribution,String source){}
    public record Result(double rawScore,double smoothedScore,Trend trend,List<Contributor> topContributors,boolean fixtureInputs,List<Long> priorSequences){}
    private final Map<String,Double> weights;private final double[] smoothing;private final double threshold;
    public ConflictEngine(@Value("${temper.conflict.weights:negativeSentiment=0.25,anger=0.20,toxicity=0.20,sarcasm=0.15,blame=0.10,defensiveness=0.10}") String configured,
            @Value("${temper.conflict.smoothing:0.6,0.3,0.1}") String smooth,@Value("${temper.conflict.trend-threshold:0.08}") double threshold){
        var parsed=new TreeMap<String,Double>();
        for(String entry:configured.split(",")){var pair=entry.trim().split("=");if(pair.length!=2||parsed.put(pair[0].trim(),Double.parseDouble(pair[1]))!=null)throw new IllegalArgumentException("Invalid conflict weights");}
        if(!parsed.keySet().equals(Set.of("negativeSentiment","anger","toxicity","sarcasm","blame","defensiveness")))throw new IllegalArgumentException("Conflict weights must specify all six signals");
        if(parsed.values().stream().anyMatch(v->!Double.isFinite(v)||v<0)||Math.abs(parsed.values().stream().mapToDouble(Double::doubleValue).sum()-1)>1e-9)throw new IllegalArgumentException("Conflict weights must be nonnegative and sum to one");
        smoothing=Arrays.stream(smooth.split(",")).mapToDouble(Double::parseDouble).toArray();
        if(smoothing.length!=3||smoothing[0]<=0||Arrays.stream(smoothing).anyMatch(v->!Double.isFinite(v)||v<0)||Math.abs(Arrays.stream(smoothing).sum()-1)>1e-9)throw new IllegalArgumentException("Smoothing requires three nonnegative weights summing to one, current positive");
        if(!Double.isFinite(threshold)||threshold<=0||threshold>1)throw new IllegalArgumentException("Invalid trend threshold");
        this.weights=Collections.unmodifiableMap(parsed);this.threshold=threshold;
    }
    private String source(MessageAnalysis row,String signal){
        String label=switch(signal){case "anger"->"GoEmotions emotion";case "negativeSentiment"->"RoBERTa sentiment";case "toxicity"->"Toxicity classifier";case "sarcasm"->"Sarcasm classifier";default->"Estimated "+signal;};
        return row.evidence().stream().filter(e->e.label().equals(label)).map(e->e.source().name()).findFirst().orElse("MOCK");
    }
    private List<Contributor> contributions(MessageAnalysis row){
        return weights.entrySet().stream().map(e->{double value=e.getKey().equals("anger")?row.emotions().get("anger"):row.signals().get(e.getKey());return new Contributor(e.getKey(),value,e.getValue(),value*e.getValue(),source(row,e.getKey()));})
            .sorted(Comparator.comparingDouble(Contributor::contribution).reversed().thenComparing(Contributor::signal)).toList();
    }
    private double raw(MessageAnalysis row){return Math.min(1,contributions(row).stream().mapToDouble(Contributor::contribution).sum());}
    private double smoothed(MessageAnalysis row,Map<Long,MessageAnalysis> prior){
        double total=raw(row)*smoothing[0],used=smoothing[0];
        for(int lag=1;lag<=2;lag++){var preceding=prior.get(row.sequence()-lag);if(preceding!=null){total+=raw(preceding)*smoothing[lag];used+=smoothing[lag];}}
        return total/used;
    }
    public Result calculate(MessageAnalysis current,List<MessageAnalysis> history){
        var prior=new TreeMap<Long,MessageAnalysis>();
        for(var row:history)if(row.sequence()<current.sequence()){if(prior.put(row.sequence(),row)!=null)throw new IllegalArgumentException("Duplicate analysis sequence");}
        var parts=contributions(current);double score=smoothed(current,prior);var previous=prior.get(current.sequence()-1);
        double delta=previous==null?0:score-smoothed(previous,prior);
        var trend=delta>=threshold?Trend.UP:delta<=-threshold?Trend.DOWN:Trend.FLAT;
        boolean fixtures=parts.stream().anyMatch(p->p.weight()>0&&p.source().equals("MOCK"));
        for(int lag=1;lag<=2;lag++){var row=prior.get(current.sequence()-lag);if(row!=null&&smoothing[lag]>0)fixtures|=contributions(row).stream().anyMatch(p->p.weight()>0&&p.source().equals("MOCK"));}
        return new Result(raw(current),score,trend,parts,fixtures,prior.keySet().stream().filter(seq->seq>=current.sequence()-2).toList());
    }
}
