package dev.temper.android;

import dev.temper.android.adapters.ScreenObservation.Bounds;
import dev.temper.android.overlay.OverlayPlacement;

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
    }
}
