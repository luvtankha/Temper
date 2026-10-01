package dev.temper.cerebro.learning;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.regex.Pattern;

/** Reviewed conversation segments. Model predictions are explicitly not human labels. */
public record LearningSubmission(String sessionId,String language,String appVersion,String modelHash,int qualityRating,
        List<Turn> turns,List<Estimate> analyses) {
    public static final List<String> LABELS=List.of("neutral","happy","concerned","confused","sad","frustrated","angry","surprised");
    public record Turn(int index,String role,String text){}
    public record Estimate(int target,List<Integer> context,List<Double> scores,String currentState,String direction,String trajectory){}
    public static LearningSubmission parse(JsonNode body){
        if(body==null||!body.isObject())throw new IllegalArgumentException("Invalid submission");
        if(!body.path("consentVersion").isIntegralNumber()||body.path("consentVersion").asLong()!=1||!affirmative(body,"adultConfirmed")||!affirmative(body,"participantPermissionConfirmed")||!affirmative(body,"textReviewed"))throw new IllegalArgumentException("Review and consent required");
        String id=body.path("sessionId").asText(),language=body.path("language").asText(),app=body.path("appVersion").asText(),hash=body.path("modelHash").asText();
        JsonNode rawRating=body.path("qualityRating");if(!rawRating.isIntegralNumber()||!rawRating.canConvertToInt())throw new IllegalArgumentException("Rate analysis quality");int rating=rawRating.asInt();if(rating<1||rating>5)throw new IllegalArgumentException("Rate analysis quality");
        if(!id.matches("[a-f0-9-]{36}")||!UUID.fromString(id).toString().equals(id)||!Set.of("ENGLISH","HINGLISH","OTHER").contains(language)||!app.matches("[0-9.]{3,24}")||!hash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid metadata");
        JsonNode input=body.path("turns"),estimates=body.path("analyses");if(!input.isArray()||input.size()<3||input.size()>64||!estimates.isArray()||estimates.isEmpty()||estimates.size()>32)throw new IllegalArgumentException("Invalid session bounds");
        List<Turn> turns=new ArrayList<>();for(int i=0;i<input.size();i++){JsonNode turn=input.get(i);String role=turn.path("role").asText(),text=turn.path("text").asText();if(!turn.path("index").isIntegralNumber()||turn.path("index").asLong(-1)!=i||!Set.of("LOCAL","REMOTE").contains(role)||!turn.path("text").isTextual()||text.isBlank()||text.length()>1000)throw new IllegalArgumentException("Invalid turn");turns.add(new Turn(i,role,redact(text)));}
        if(turns.stream().noneMatch(t->t.role.equals("LOCAL"))||turns.stream().noneMatch(t->t.role.equals("REMOTE")))throw new IllegalArgumentException("Both roles required");
        Set<Integer> targets=new HashSet<>();List<Estimate> analyses=new ArrayList<>();
        for(JsonNode estimate:estimates){
            JsonNode rawTarget=estimate.path("target");if(!rawTarget.isIntegralNumber()||!rawTarget.canConvertToInt())throw new IllegalArgumentException("Invalid analysis target");int target=rawTarget.asInt();if(target<0||target>=turns.size()||!turns.get(target).role.equals("REMOTE")||!targets.add(target))throw new IllegalArgumentException("Invalid analysis target");
            JsonNode rawScores=estimate.path("scores"),rawContext=estimate.path("context");if(!rawScores.isArray()||rawScores.size()!=8||!rawContext.isArray()||rawContext.size()<3||rawContext.size()>8)throw new IllegalArgumentException("Invalid scores/context");
            List<Integer> context=new ArrayList<>();int lastRemote=-1;boolean local=false;for(JsonNode index:rawContext){if(!index.isIntegralNumber()||!index.canConvertToInt()||index.asInt()<0||index.asInt()>=turns.size()||context.contains(index.asInt()))throw new IllegalArgumentException("Invalid context index");int value=index.asInt();context.add(value);if(turns.get(value).role.equals("REMOTE"))lastRemote=value;else local=true;}if(lastRemote!=target||!local)throw new IllegalArgumentException("Target must be the latest remote turn");
            String current=estimate.path("currentState").asText(),direction=estimate.path("direction").asText(),trajectory=estimate.path("trajectory").asText();if(current.isBlank()||current.length()>64||direction.isBlank()||direction.length()>64||!Set.of("UNCERTAIN","STABLE","TENSION_RISING","DEESCALATING","ESCALATION_RISK").contains(trajectory))throw new IllegalArgumentException("Invalid summary");
            List<Double> scores=new ArrayList<>();for(JsonNode value:rawScores){if(!value.isNumber()||!Double.isFinite(value.asDouble())||value.asDouble()<0||value.asDouble()>1)throw new IllegalArgumentException("Invalid score");scores.add(value.asDouble());}analyses.add(new Estimate(target,List.copyOf(context),List.copyOf(scores),current,direction,trajectory));
        }
        return new LearningSubmission(id,language,app,hash,rating,List.copyOf(turns),List.copyOf(analyses));
    }
    private static final Pattern CONTACT=Pattern.compile("(?i)https?://\\S+|[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}|(?<!\\w)@\\w+|(?<!\\w)(?:\\+?\\d[\\d ()-]{6,}\\d)(?!\\w)");
    private static boolean affirmative(JsonNode body,String field){return body.path(field).isBoolean()&&body.path(field).booleanValue();}
    public static String redact(String text){return CONTACT.matcher(text).replaceAll("[redacted]").replaceAll("[\\p{Cc}&&[^\\n\\t]]","");}
}
