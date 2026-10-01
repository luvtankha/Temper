package dev.temper.android.adapters;

/** Deduplicates repeated content windows, not historical messages across scrolling. */
public final class VisibleSnapshotDeduplicator {
    private String previous;
    public boolean accept(VisibleConversation snapshot){
        if(snapshot.status()!=VisibleConversation.Status.AVAILABLE){clear();return false;}
        StringBuilder key=new StringBuilder(snapshot.conversationKey());for(var turn:snapshot.turns())key.append(':').append(turn.key());
        String value=key.toString();if(value.equals(previous))return false;previous=value;return true;
    }
    public void clear(){previous=null;}
}
