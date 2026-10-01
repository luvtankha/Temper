package dev.temper.android.accessibility;

import android.view.accessibility.AccessibilityNodeInfo;
import android.graphics.Rect;
import dev.temper.android.adapters.*;
import java.util.*;

/** Reads only planned message/date/header fields; never composer drafts or descriptions. */
public final class WhatsAppTextReader {
    public VisibleConversation read(AccessibilityNodeInfo root,WhatsAppAdapter adapter){
        return read(root,adapter,key->true);
    }
    public VisibleConversation read(AccessibilityNodeInfo root,WhatsAppAdapter adapter,java.util.function.Predicate<String> identityAllowed){
        ScreenObservation structure=new WhatsAppStructureProbe().read(root);WhatsAppAdapter.ReadPlan plan=adapter.plan(structure);
        if(plan.status()!=VisibleConversation.Status.AVAILABLE)return VisibleConversation.unavailable(plan.status());
        // Verify the opaque selected identity before any message/date fields are read.
        Map<Integer,String> identity=new HashMap<>();int[] identityIndex={0},identityVisited={0};
        walk(root,structure,Map.of(plan.identity(),128),identity,identityIndex,identityVisited,0);
        if(identityIndex[0]!=structure.nodes().size()||!identityAllowed.test(adapter.conversationKey(identity.get(plan.identity()))))return VisibleConversation.unavailable(VisibleConversation.Status.NOT_CONVERSATION);
        Map<Integer,Integer> wanted=new HashMap<>();wanted.put(plan.identity(),128);for(var row:plan.rows()){wanted.put(row.message(),1000);wanted.put(row.date(),32);}
        Map<Integer,String> texts=new HashMap<>();int[] index={0},visited={0};walk(root,structure,wanted,texts,index,visited,0);
        if(index[0]!=structure.nodes().size()||!Objects.equals(identity.get(plan.identity()),texts.get(plan.identity())))return VisibleConversation.unavailable(VisibleConversation.Status.UNSUPPORTED_LAYOUT);
        List<ScreenObservation.Node> enriched=new ArrayList<>();for(int i=0;i<structure.nodes().size();i++){var node=structure.nodes().get(i);enriched.add(new ScreenObservation.Node(node.parentIndex(),node.resourceId(),node.className(),node.bounds(),node.visible(),node.editable(),node.password(),texts.get(i)));}
        return adapter.read(new ScreenObservation(structure.packageName(),structure.viewport(),enriched));
    }
    private void walk(AccessibilityNodeInfo node,ScreenObservation structure,Map<Integer,Integer> wanted,Map<Integer,String> texts,int[] index,int[] visited,int depth){
        if(depth>24||++visited[0]>512)throw new IllegalArgumentException("Traversal limit");
        CharSequence pkg=node.getPackageName();if(pkg!=null&&!"com.whatsapp".contentEquals(pkg))return;if(node.isPassword()||!node.isVisibleToUser())return;
        Rect rect=new Rect();node.getBoundsInScreen(rect);var viewport=structure.viewport();rect.left=Math.max(rect.left,viewport.left());rect.top=Math.max(rect.top,viewport.top());rect.right=Math.min(rect.right,viewport.right());rect.bottom=Math.min(rect.bottom,viewport.bottom());
        if(rect.width()>0&&rect.height()>0){
            int position=index[0]++;if(position>=structure.nodes().size())throw new IllegalArgumentException("Layout changed");
            var expected=structure.nodes().get(position);String id=node.getViewIdResourceName();String actual=id==null?"":id;
            if(!expected.resourceId().equals(actual)||!expected.bounds().equals(new ScreenObservation.Bounds(rect.left,rect.top,rect.right,rect.bottom)))throw new IllegalArgumentException("Layout changed");
            if(wanted.containsKey(position)){
                if(node.isEditable())throw new IllegalArgumentException("Editable content excluded");
                CharSequence value=node.getText();if(value==null||value.length()>wanted.get(position))throw new IllegalArgumentException("Text limit");texts.put(position,value.toString());
            }
        }
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null){try{walk(child,structure,wanted,texts,index,visited,depth+1);}finally{child.recycle();}}}
    }
}
