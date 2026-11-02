package dev.spaceclient;
import dev.spaceclient.mods.SnaplookState;
import org.junit.Test;
import static org.junit.Assert.*;
public class SnaplookStateTest {
    @Test public void everyPerspectiveRendersTemporarilyAndRestoresItsOriginalView() {
        for(int current=0;current<3;current++)for(int target=0;target<3;target++) {
            SnaplookState s=new SnaplookState();assertEquals(target,s.begin(current,true,true,target));assertTrue(s.active());
            assertEquals(current,s.end(target));assertFalse(s.active());assertEquals(current,s.end(current));
        }
    }
    @Test public void manualF5ChangeBetweenFramesSurvivesHoldingAndReleasingSnaplook() {
        SnaplookState s=new SnaplookState();assertEquals(2,s.begin(0,true,true,2));assertEquals(0,s.end(2));
        assertEquals(2,s.begin(1,true,true,2));assertEquals(1,s.end(2));
        assertEquals(1,s.begin(1,true,false,2));assertFalse(s.active());assertEquals(1,s.end(1));
    }
    @Test public void lostContextAndInterruptedFramesReleaseThePreviousPerspectiveLease() {
        SnaplookState s=new SnaplookState();assertEquals(2,s.begin(1,true,true,2));
        assertEquals(1,s.begin(2,false,true,0));assertFalse(s.active());
        assertEquals(2,s.begin(0,true,true,2));assertEquals(1,s.begin(2,true,true,1));assertEquals(0,s.end(1));
    }
    @Test public void anotherViewChangeDuringRenderingIsKeptRatherThanOverwritten() {
        SnaplookState s=new SnaplookState();assertEquals(2,s.begin(0,true,true,2));assertEquals(1,s.end(1));assertFalse(s.active());
    }
    @Test public void unsupportedViewsAndDisabledOrReleasedInputLeavePerspectiveAlone() {
        SnaplookState s=new SnaplookState();assertEquals(3,s.begin(3,true,true,2));assertFalse(s.active());
        assertEquals(0,s.begin(0,true,true,-1));assertFalse(s.active());
        assertEquals(2,s.begin(2,false,true,1));assertEquals(2,s.begin(2,true,false,1));assertFalse(s.active());
    }
}
