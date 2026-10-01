package dev.temper.android.adapters;

import java.util.List;
import java.util.Objects;

/** Only whitelisted relevant nodes may carry text; caller caps extraction before construction. */
public record ScreenObservation(String packageName, Bounds viewport, List<Node> nodes) {
    public ScreenObservation {
        Objects.requireNonNull(packageName);Objects.requireNonNull(viewport);nodes=List.copyOf(nodes);
        if(nodes.size()>256)throw new IllegalArgumentException("Observation node limit");
        for(int i=0;i<nodes.size();i++)if(nodes.get(i).parentIndex()>=i)throw new IllegalArgumentException("Parent must precede child");
    }
    public record Bounds(int left,int top,int right,int bottom){
        public Bounds{if(left<0||top<0||right<=left||bottom<=top)throw new IllegalArgumentException("Invalid bounds");}
        public boolean contains(Bounds other){return other.left>=left&&other.top>=top&&other.right<=right&&other.bottom<=bottom;}
    }
    public record Node(int parentIndex,String resourceId,String className,Bounds bounds,boolean visible,boolean editable,boolean password,String text){
        public Node {if(parentIndex< -1)throw new IllegalArgumentException("Invalid parent");Objects.requireNonNull(resourceId);Objects.requireNonNull(className);Objects.requireNonNull(bounds);if(text!=null&&text.length()>1000)throw new IllegalArgumentException("Node text limit");}
        @Override public String toString(){return "Node[id="+resourceId+", parent="+parentIndex+", text=redacted]";}
    }
    @Override public String toString(){return "ScreenObservation[package="+packageName+", nodes="+nodes.size()+"]";}
}
