package dev.temper.android.adapters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import dev.temper.android.privacy.ConsentStore;
import dev.temper.android.adapters.VisibleConversation.*;

/** Conservative portrait text-row profile observed on WhatsApp 2.26.37.73/API34. */
public final class WhatsAppAdapter implements ChatPlatformAdapter {
    public static final String PREFIX="com.whatsapp:id/";
    public record Row(int node,int message,int date,Role role){}
    public record ReadPlan(Status status,List<Row> rows,int identity,ScreenObservation.Bounds composer){public ReadPlan{rows=List.copyOf(rows);}}
    private final String sessionSalt;
    public WhatsAppAdapter(String sessionSalt){this.sessionSalt=Objects.requireNonNull(sessionSalt);if(sessionSalt.length()<16)throw new IllegalArgumentException("Session salt required");}
    public String conversationKey(String title){if(title==null||title.isBlank()||title.length()>128)throw new IllegalArgumentException("Invalid conversation identity");return hash(sessionSalt+"\u0000"+title);}
    @Override public boolean supports(String packageName){return ConsentStore.WHATSAPP.equals(packageName);}
    private ReadPlan fail(Status status){return new ReadPlan(status,List.of(),-1,null);}
    private int unique(ScreenObservation screen,String id){int found=-1;for(int i=0;i<screen.nodes().size();i++)if(id.equals(screen.nodes().get(i).resourceId())){if(found!=-1)return -2;found=i;}return found;}
    /** Composer verification is independent of whether message content can be analyzed. */
    public ReadPlan anchor(ScreenObservation screen){
        if(!supports(screen.packageName()))return fail(Status.UNSUPPORTED_PACKAGE);
        int entry=unique(screen,PREFIX+"entry"),list=unique(screen,"android:id/list"),identity=unique(screen,PREFIX+"conversation_contact_name");
        if(entry<0||list<0||identity<0)return fail(Status.NOT_CONVERSATION);
        var composer=screen.nodes().get(entry);var listNode=screen.nodes().get(list);var header=screen.nodes().get(identity);
        if(!composer.editable()||composer.password()||!composer.visible()||!header.visible()||!composer.className().equals("android.widget.EditText")||!listNode.className().equals("android.widget.ListView")||!header.className().equals("android.widget.TextView")||!screen.viewport().contains(composer.bounds())||composer.bounds().top()<listNode.bounds().bottom()||header.bounds().bottom()>listNode.bounds().top()||screen.viewport().right()-screen.viewport().left()>=screen.viewport().bottom()-screen.viewport().top())return fail(Status.UNSUPPORTED_LAYOUT);
        return new ReadPlan(Status.AVAILABLE,List.of(),identity,composer.bounds());
    }
    public ReadPlan plan(ScreenObservation screen){
        ReadPlan anchor=anchor(screen);if(anchor.status()!=Status.AVAILABLE)return anchor;
        int list=unique(screen,"android:id/list"),identity=anchor.identity();var listNode=screen.nodes().get(list);
        List<Row> rows=new ArrayList<>();
        for(int i=0;i<screen.nodes().size();i++){
            var row=screen.nodes().get(i);
            if(row.parentIndex()!=list||!row.resourceId().startsWith(PREFIX+"conversation_row_"))continue;
            // Date separators are list metadata, never message text or sender evidence.
            if(row.resourceId().equals(PREFIX+"conversation_row_date_divider")){
                if(!row.className().equals("android.widget.TextView")||row.editable()||row.password())return fail(Status.UNSUPPORTED_LAYOUT);
                for(var child:screen.nodes())if(child.parentIndex()==i)return fail(Status.UNSUPPORTED_LAYOUT);
                continue;
            }
            // A row touching either list edge may have clipped sender/timestamp evidence.
            if(!row.visible()||row.bounds().top()<=listNode.bounds().top()+1||row.bounds().bottom()>=listNode.bounds().bottom()-1)continue;
            if(!row.resourceId().equals(PREFIX+"conversation_row_text"))return fail(Status.UNSUPPORTED_LAYOUT);
            int message=-1,date=-1,status=-1;
            for(int j=i+1;j<screen.nodes().size();j++){
                var child=screen.nodes().get(j);if(child.parentIndex()!=i)continue;
                if(!row.bounds().contains(child.bounds())||child.password()||!child.visible())return fail(Status.UNSUPPORTED_LAYOUT);
                String id=child.resourceId();
                if(id.equals(PREFIX+"message_text")){if(message>=0)return fail(Status.UNSUPPORTED_LAYOUT);message=j;}
                else if(id.equals(PREFIX+"date")){if(date>=0)return fail(Status.UNSUPPORTED_LAYOUT);date=j;}
                else if(id.equals(PREFIX+"status")){if(status>=0)return fail(Status.UNSUPPORTED_LAYOUT);status=j;}
                else if(!id.isEmpty())return fail(Status.UNSUPPORTED_LAYOUT);
            }
            if(message<0||date<0)return fail(Status.UNSUPPORTED_LAYOUT);
            var body=screen.nodes().get(message);var time=screen.nodes().get(date);int width=screen.viewport().right()-screen.viewport().left();
            if(body.editable()||!body.className().equals("android.widget.TextView")||!time.className().equals("android.widget.TextView"))return fail(Status.UNSUPPORTED_LAYOUT);
            Role role;
            if(status>=0){var marker=screen.nodes().get(status);if(!marker.className().equals("android.widget.ImageView")||marker.bounds().left()<screen.viewport().left()+width*.8||body.bounds().left()<screen.viewport().left()+width*.12)return fail(Status.AMBIGUOUS_ROLES);role=Role.LOCAL;}
            else{if(body.bounds().left()<screen.viewport().left()+width*.03||body.bounds().left()>screen.viewport().left()+width*.08)return fail(Status.AMBIGUOUS_ROLES);role=Role.REMOTE;}
            rows.add(new Row(i,message,date,role));
        }
        rows.sort(Comparator.comparingInt(row->screen.nodes().get(row.node()).bounds().top()));
        if(rows.isEmpty())return fail(Status.NOT_CONVERSATION);
        if(rows.size()>8)rows=new ArrayList<>(rows.subList(rows.size()-8,rows.size()));
        return new ReadPlan(Status.AVAILABLE,rows,identity,anchor.composer());
    }
    @Override public VisibleConversation read(ScreenObservation screen){
        ReadPlan plan=plan(screen);if(plan.status()!=Status.AVAILABLE)return VisibleConversation.unavailable(plan.status());
        String title=screen.nodes().get(plan.identity()).text();if(title==null||title.isBlank()||title.length()>128)return VisibleConversation.unavailable(Status.NOT_CONVERSATION);
        String conversation=conversationKey(title);List<Turn> turns=new ArrayList<>();Map<String,Integer> duplicates=new HashMap<>();
        for(Row row:plan.rows()){
            String body=screen.nodes().get(row.message()).text(),date=screen.nodes().get(row.date()).text();
            if(body==null||body.isBlank()||body.length()>1000||date==null||date.isBlank()||date.length()>32)return VisibleConversation.unavailable(Status.LIMIT_EXCEEDED);
            String base=hash(conversation+"\u0000"+row.role()+"\u0000"+body+"\u0000"+date);int occurrence=duplicates.merge(base,1,Integer::sum);
            turns.add(new Turn(hash(base+"\u0000"+occurrence),row.role(),body));
        }
        return new VisibleConversation(Status.AVAILABLE,conversation,turns,plan.composer());
    }
    public static String hash(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder result=new StringBuilder();for(byte item:bytes)result.append(String.format(Locale.ROOT,"%02x",item&255));return result.toString();}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException("SHA-256 unavailable",impossible);}}
}
