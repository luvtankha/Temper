package dev.temper.android.adapters;

import java.util.List;
import java.util.Objects;

public record VisibleConversation(Status status,String conversationKey,List<Turn> turns,ScreenObservation.Bounds composer) {
    public enum Status {AVAILABLE,UNSUPPORTED_PACKAGE,NOT_CONVERSATION,AMBIGUOUS_ROLES,UNSUPPORTED_LAYOUT,LIMIT_EXCEEDED}
    public enum Role {LOCAL,REMOTE}
    public record Turn(String key,Role role,String text){
        public Turn{Objects.requireNonNull(key);Objects.requireNonNull(role);Objects.requireNonNull(text);if(!key.matches("[a-f0-9]{64}")||text.isBlank()||text.length()>1000)throw new IllegalArgumentException("Invalid turn");}
        @Override public String toString(){return "Turn[role="+role+", text=redacted]";}
    }
    public VisibleConversation {
        Objects.requireNonNull(status);turns=List.copyOf(turns);
        if(status==Status.AVAILABLE){if(conversationKey==null||!conversationKey.matches("[a-f0-9]{64}")||turns.isEmpty()||turns.size()>8||composer==null)throw new IllegalArgumentException("Incomplete conversation");}
        else if(conversationKey!=null||!turns.isEmpty()||composer!=null)throw new IllegalArgumentException("Unavailable results cannot carry content");
    }
    public static VisibleConversation unavailable(Status reason){if(reason==Status.AVAILABLE)throw new IllegalArgumentException("Expected failure");return new VisibleConversation(reason,null,List.of(),null);}
    @Override public String toString(){return "VisibleConversation[status="+status+", turns="+turns.size()+"]";}
}
