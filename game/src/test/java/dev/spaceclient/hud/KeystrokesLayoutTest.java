package dev.spaceclient.hud;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class KeystrokesLayoutTest {
    private KeystrokesHud.Key[] keys(boolean jump,boolean mouse,boolean pressed,String forward) {
        List<KeystrokesHud.Key> result=new ArrayList<KeystrokesHud.Key>();
        for(String label:new String[]{forward,"A","S","D"})result.add(new KeystrokesHud.Key(label,pressed));
        if(jump)result.add(new KeystrokesHud.Key("SPACE",pressed));
        if(mouse){result.add(new KeystrokesHud.Key("M1",pressed));result.add(new KeystrokesHud.Key("M2",pressed));}
        return result.toArray(new KeystrokesHud.Key[0]);
    }
    @Test public void everyCombinationKeepsCellsWithinBoundsAndWithoutOverlap() {
        for(boolean jump:new boolean[]{false,true})for(boolean mouse:new boolean[]{false,true}) {
            KeystrokesLayout l=KeystrokesLayout.block(keys(jump,mouse,false,"W"),jump,mouse,s->s.length()*6);
            assertEquals(4+(jump?1:0)+(mouse?2:0),l.cells.size());Set<Integer> indices=new HashSet<Integer>();
            for(KeystrokesLayout.Cell c:l.cells) {
                assertTrue(indices.add(c.keyIndex));assertTrue(c.x>=0&&c.y>=0);
                assertTrue(c.x+c.width<=l.width&&c.y+c.height<=l.height);
                for(KeystrokesLayout.Cell other:l.cells)if(other!=c)
                    assertFalse(c.x<other.x+other.width&&c.x+c.width>other.x&&c.y<other.y+other.height&&c.y+c.height>other.y);
            }
            assertEquals(l.cells.get(2).x,l.cells.get(0).x);assertEquals(0,l.cells.get(0).y);
            if(jump)assertEquals(l.width,l.cells.get(4).width);
        }
    }
    @Test public void pressedStateAndLongRemappedNamesCannotShiftOrUnboundTheGrid() {
        KeystrokesLayout a=KeystrokesLayout.block(keys(true,true,false,"VERY LONG KEY NAME"),true,true,s->s.length()*6);
        KeystrokesLayout b=KeystrokesLayout.block(keys(true,true,true,"VERY LONG KEY NAME"),true,true,s->s.length()*6);
        assertEquals(a.width,b.width);assertEquals(a.height,b.height);assertTrue(a.width<=124);
        for(int i=0;i<a.cells.size();i++){assertEquals(a.cells.get(i).x,b.cells.get(i).x);assertEquals(a.cells.get(i).width,b.cells.get(i).width);}
    }
    @Test public void fullHeightParticipatesInFittingNudgingAndAllFourAnchors() {
        KeystrokesLayout l=KeystrokesLayout.block(keys(true,true,false,"W"),true,true,s->s.length()*6);
        for(int[] screen:new int[][]{{320,240},{427,240},{640,360}})for(double scale:new double[]{.5,1,3}) {
            HudPlacement p=new HudPlacement(HudPlacement.Horizontal.LEFT,HudPlacement.Vertical.TOP,12,15,scale);
            HudPlacement.Bounds b=p.resolve(screen[0],screen[1],l.width,l.height);
            assertTrue(b.y+b.height<=screen[1]);assertEquals(l.height*b.scale,b.height,0);
            for(HudPlacement.Horizontal hx:HudPlacement.Horizontal.values())for(HudPlacement.Vertical vy:HudPlacement.Vertical.values()) {
                HudPlacement anchored=p.anchored(hx,vy,screen[0],screen[1],l.width,l.height);
                HudPlacement.Bounds unchanged=anchored.resolve(screen[0],screen[1],l.width,l.height);
                assertEquals(b.x,unchanged.x,.0001);assertEquals(b.y,unchanged.y,.0001);
            }
            HudPlacement.Bounds moved=p.at(999,999,screen[0],screen[1],l.width,l.height,false).resolve(screen[0],screen[1],l.width,l.height);
            assertEquals(screen[0],moved.x+moved.width,.0001);assertEquals(screen[1],moved.y+moved.height,.0001);
        }
    }
}
