package dev.temper.android.accessibility;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import dev.temper.android.adapters.ScreenObservation;
import dev.temper.android.privacy.ConsentStore;
import java.util.ArrayList;
import java.util.List;

/** One-shot layout calibration. Deliberately never calls getText/getContentDescription. */
public final class WhatsAppStructureProbe {
    private final List<ScreenObservation.Node> nodes=new ArrayList<>();
    private ScreenObservation.Bounds viewport;
    private int visited;
    public ScreenObservation read(AccessibilityNodeInfo root){
        if(root==null||!ConsentStore.WHATSAPP.contentEquals(root.getPackageName()==null?"":root.getPackageName()))throw new IllegalArgumentException("Unsupported root");
        nodes.clear();visited=0;Rect rect=new Rect();root.getBoundsInScreen(rect);viewport=bounds(rect);
        visit(root,-1,0);return new ScreenObservation(ConsentStore.WHATSAPP,viewport,nodes);
    }
    private void visit(AccessibilityNodeInfo node,int parent,int depth){
        if(depth>24||++visited>512)throw new IllegalArgumentException("Traversal limit");
        CharSequence pkg=node.getPackageName();if(pkg!=null&&!ConsentStore.WHATSAPP.contentEquals(pkg))return;
        if(node.isPassword()||!node.isVisibleToUser())return;
        Rect rect=new Rect();node.getBoundsInScreen(rect);
        rect.left=Math.max(rect.left,viewport.left());rect.top=Math.max(rect.top,viewport.top());rect.right=Math.min(rect.right,viewport.right());rect.bottom=Math.min(rect.bottom,viewport.bottom());
        int nextParent=parent;
        if(rect.width()>0&&rect.height()>0){
            if(nodes.size()>=256)throw new IllegalArgumentException("Observation limit");
            String id=node.getViewIdResourceName();CharSequence className=node.getClassName();nextParent=nodes.size();
            nodes.add(new ScreenObservation.Node(parent,id==null?"":id,className==null?"":className.toString(),bounds(rect),true,node.isEditable(),false,null));
        }
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null){try{visit(child,nextParent,depth+1);}finally{child.recycle();}}}
    }
    private static ScreenObservation.Bounds bounds(Rect rect){return new ScreenObservation.Bounds(rect.left,rect.top,rect.right,rect.bottom);}
}
