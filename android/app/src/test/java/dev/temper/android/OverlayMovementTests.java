package dev.temper.android;

import dev.temper.android.adapters.ScreenObservation.Bounds;
import dev.temper.android.overlay.DragGesture;
import dev.temper.android.overlay.DraggablePlacement;
import dev.temper.android.overlay.PopupPlacement;
import org.junit.Test;
import static org.junit.Assert.*;

public class OverlayMovementTests {
    private final Bounds viewport=new Bounds(0,103,1080,2352),closedComposer=new Bounds(168,2218,452,2315),openComposer=new Bounds(168,1300,452,1397);
    @Test public void draggedPreferenceSurvivesKeyboardClampAndRecreation(){
        Bounds chosen=DraggablePlacement.move(viewport,closedComposer,3,800,1850);
        var saved=DraggablePlacement.preference(viewport,chosen);
        Bounds open=DraggablePlacement.place(viewport,openComposer,3,saved.x(),saved.y());
        assertEquals(chosen.left(),open.left());assertEquals(openComposer.top(),open.bottom());
        assertEquals(chosen,DraggablePlacement.place(viewport,closedComposer,3,saved.x(),saved.y()));
    }
    @Test public void movementStaysInVisibleChatAcrossDensitiesAndOrigins(){
        for(float density:new float[]{1,2,3})for(float x:new float[]{-10000,0,100,10000})for(float y:new float[]{-10000,0,800,10000}){
            Bounds moved=DraggablePlacement.move(viewport,openComposer,density,x,y);
            assertTrue(viewport.contains(moved));assertTrue(moved.bottom()<=openComposer.top());
            var saved=DraggablePlacement.preference(viewport,moved);assertEquals(moved,DraggablePlacement.place(viewport,openComposer,density,saved.x(),saved.y()));
        }
        assertEquals(103,DraggablePlacement.place(viewport,closedComposer,3,1,-2).top());
        assertEquals(1080,DraggablePlacement.place(viewport,closedComposer,3,2,1).right());
    }
    @Test public void movementRejectsInvalidGeometryAndPreferences(){
        assertThrows(IllegalArgumentException.class,()->DraggablePlacement.place(viewport,closedComposer,3,Float.NaN,0));
        assertThrows(IllegalArgumentException.class,()->DraggablePlacement.move(viewport,closedComposer,3,0,Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,()->DraggablePlacement.preference(viewport,new Bounds(0,0,192,264)));
        assertThrows(IllegalArgumentException.class,()->DraggablePlacement.move(viewport,new Bounds(0,103,192,200),3,0,0));
    }
    @Test public void tapsDragsAndCancellationAreDistinct(){
        DragGesture gesture=new DragGesture(8);gesture.begin(100,100,0,1000);assertNull(gesture.move(106,100));assertEquals(DragGesture.Finish.TAP,gesture.finish());
        gesture.begin(100,100,0,1000);var moved=gesture.move(120,130);assertEquals(20,moved.x(),0);assertEquals(1030,moved.y(),0);
        assertNotNull(gesture.move(100,100));assertEquals(DragGesture.Finish.DRAG,gesture.finish());
        gesture.begin(100,100,0,1000);assertFalse(gesture.cancel());assertEquals(DragGesture.Finish.NONE,gesture.finish());assertNull(gesture.move(200,200));
        gesture.begin(100,100,0,1000);gesture.move(200,200);assertTrue(gesture.cancel());assertEquals(DragGesture.Finish.NONE,gesture.finish());
    }
    @Test public void popupOpensAtTopAndBottomCornersWithoutCoveringComposer(){
        Bounds chat=DraggablePlacement.chatViewport(viewport,closedComposer);
        for(float x:new float[]{0,1})for(float y:new float[]{0,1}){
            Bounds character=DraggablePlacement.place(viewport,closedComposer,3,x,y);Bounds panel=PopupPlacement.place(chat,character,840,830,12);
            assertTrue(chat.contains(panel));assertEquals(830,panel.bottom()-panel.top());assertEquals(840,panel.right()-panel.left());
            if(y==0)assertTrue(panel.top()>character.bottom());else assertTrue(panel.bottom()<character.top());
        }
    }
    @Test public void popupUsesSideOrBoundedFallbackWhenNeitherVerticalSideFits(){
        Bounds chat=new Bounds(20,100,1000,1100),leftCharacter=new Bounds(20,450,212,714);
        Bounds side=PopupPlacement.place(chat,leftCharacter,300,830,12);assertEquals(224,side.left());assertTrue(chat.contains(side));
        Bounds fallback=PopupPlacement.place(chat,new Bounds(400,450,592,714),840,830,12);assertTrue(chat.contains(fallback));assertEquals(830,fallback.bottom()-fallback.top());
        assertThrows(IllegalArgumentException.class,()->PopupPlacement.place(chat,leftCharacter,840,1001,12));
    }
}
