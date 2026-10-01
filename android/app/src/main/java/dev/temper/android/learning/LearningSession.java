package dev.temper.android.learning;

import dev.temper.android.adapters.VisibleConversation;
import java.util.*;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/** Bounded opt-in session memory, never a disk queue or an automatic upload. */
public final class LearningSession {
    public static final long TTL=600_000;
    public record Turn(int index,String role,String text){@Override public String toString(){return "Turn[index="+index+", role="+role+", text=redacted]";}}
    public record Estimate(int target,List<Integer> context,float[] scores,String currentState,String direction,String trajectory){public Estimate{context=List.copyOf(context);scores=scores.clone();}@Override public float[] scores(){return scores.clone();}}
    public record Review(String id,List<Turn> turns,List<Estimate> analyses){public Review{turns=List.copyOf(turns);analyses=List.copyOf(analyses);}@Override public String toString(){return "Review[turns="+turns.size()+", estimates="+analyses.size()+"]";}}
    private final LongSupplier clock;private long expires;private String id,conversation;private boolean collecting;
    private final LinkedHashMap<String,Turn> turns=new LinkedHashMap<>();private final LinkedHashMap<Integer,Estimate> estimates=new LinkedHashMap<>();
    public LearningSession(LongSupplier clock){this.clock=clock;}
    public synchronized void arm(){discard();id=UUID.randomUUID().toString();expires=clock.getAsLong()+TTL;collecting=true;}
    private void expire(){if(id!=null&&clock.getAsLong()>=expires)discard();}
    public synchronized boolean collecting(){expire();return collecting;}
    public synchronized void finish(){expire();collecting=false;}
    public synchronized void observe(VisibleConversation snapshot,float[] scores){
        observe(snapshot,scores,"Generated fixture estimate","Direction uncertain","UNCERTAIN");
    }
    public synchronized void observe(VisibleConversation snapshot,float[] scores,String currentState,String direction,String trajectory){
        expire();if(!collecting||snapshot.status()!=VisibleConversation.Status.AVAILABLE)return;if(conversation==null)conversation=snapshot.conversationKey();if(!conversation.equals(snapshot.conversationKey())){finish();return;}
        for(var turn:snapshot.turns())if(!turns.containsKey(turn.key())){if(turns.size()>=64){finish();return;}turns.put(turn.key(),new Turn(turns.size(),turn.role().name(),redact(turn.text())));}
        var remote=snapshot.turns().stream().filter(t->t.role()==VisibleConversation.Role.REMOTE).toList();if(remote.isEmpty())return;int target=turns.get(remote.get(remote.size()-1).key()).index();if(estimates.size()>=32&&!estimates.containsKey(target)){finish();return;}
        if(scores!=null&&scores.length==8&&currentState!=null&&direction!=null&&trajectory!=null&&currentState.length()<=64&&direction.length()<=64&&trajectory.length()<=32){for(float score:scores)if(!Float.isFinite(score)||score<0||score>1)return;estimates.put(target,new Estimate(target,snapshot.turns().stream().map(t->turns.get(t.key()).index()).toList(),scores,currentState,direction,trajectory));}
    }
    public synchronized Review review(){expire();return id==null?null:new Review(id,List.copyOf(turns.values()),List.copyOf(estimates.values()));}
    public synchronized long remainingMillis(String expectedId){expire();return id!=null&&id.equals(expectedId)?Math.max(0,expires-clock.getAsLong()):0;}
    public synchronized boolean discard(String expectedId){expire();if(id==null||!id.equals(expectedId))return false;discard();return true;}
    public synchronized void discard(){id=null;conversation=null;expires=0;collecting=false;turns.clear();estimates.clear();}
    private static final Pattern CONTACT=Pattern.compile("(?i)https?://\\S+|[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}|(?<!\\w)@\\w+|(?<!\\w)(?:\\+?\\d[\\d ()-]{6,}\\d)(?!\\w)");
    public static String redact(String text){String redacted=CONTACT.matcher(text).replaceAll("[redacted]").replaceAll("[\\p{Cc}&&[^\\n\\t]]","");return redacted.length()<=1000?redacted:redacted.substring(0,1000);}
}
