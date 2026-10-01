package dev.temper.android;

import dev.temper.android.adapters.ScreenObservation.Bounds;
import dev.temper.android.overlay.OverlayPlacement;
import dev.temper.android.overlay.DraggablePlacement;
import dev.temper.android.overlay.DragGesture;

final class OverlayPlacementChecks {
    static void run(){
        Bounds viewport=new Bounds(0,0,1080,2400),composer=new Bounds(168,2218,452,2315);
        Bounds closed=OverlayPlacement.place(viewport,composer,3);
        if(closed.right()-closed.left()!=192||closed.bottom()-closed.top()!=264||closed.left()!=0||closed.bottom()!=composer.top()||!viewport.contains(closed))throw new AssertionError("Compact grounding incorrect");
        Bounds keyboard=new Bounds(168,1300,452,1397);Bounds open=OverlayPlacement.place(viewport,keyboard,3);
        if(open.bottom()!=keyboard.top()||open.left()!=closed.left()||open.top()>=closed.top())throw new AssertionError("Keyboard anchor did not move");
        Bounds small=OverlayPlacement.place(new Bounds(0,0,320,640),new Bounds(40,500,200,540),1);
        if(small.right()-small.left()!=64||small.bottom()!=500)throw new AssertionError("Density adaptation incorrect");
        try{OverlayPlacement.place(new Bounds(0,0,800,400),new Bounds(100,300,700,350),1);throw new AssertionError("Landscape accepted");}catch(IllegalArgumentException expected){}
        try{OverlayPlacement.place(viewport,new Bounds(100,100,300,200),3);throw new AssertionError("Insufficient room accepted");}catch(IllegalArgumentException expected){}
        try{OverlayPlacement.place(viewport,composer,Float.NaN);throw new AssertionError("Invalid density accepted");}catch(IllegalArgumentException expected){}
        try{OverlayPlacement.place(viewport,new Bounds(0,2400,200,2500),3);throw new AssertionError("External composer accepted");}catch(IllegalArgumentException expected){}
        Bounds dragged=DraggablePlacement.move(viewport,composer,3,880,1850);var preference=DraggablePlacement.preference(viewport,dragged);
        Bounds keyboardDragged=DraggablePlacement.place(viewport,keyboard,3,preference.x(),preference.y());
        if(keyboardDragged.left()!=dragged.left()||keyboardDragged.bottom()>keyboard.top()||!dragged.equals(DraggablePlacement.place(viewport,composer,3,preference.x(),preference.y())))throw new AssertionError("Dragging/keyboard recovery lost chosen position");
        for(float x:new float[]{-1000,0,2000})for(float y:new float[]{-1000,0,3000})if(!DraggablePlacement.chatViewport(viewport,keyboard).contains(DraggablePlacement.move(viewport,keyboard,3,x,y)))throw new AssertionError("Dragged character escaped visible chat");
        var gesture=new DragGesture(8);gesture.begin(100,100,0,1000);if(gesture.move(102,101)!=null||gesture.finish()!=DragGesture.Finish.TAP)throw new AssertionError("Touch slop broke taps");
        gesture.begin(100,100,0,1000);gesture.move(140,150);gesture.cancel();if(gesture.finish()!=DragGesture.Finish.NONE)throw new AssertionError("Canceled drag opened popup");
    }
}
