package dev.spaceclient;

import dev.spaceclient.mods.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class GameplayStateTest {
    @Test public void toggleSprintCanBeStoppedAndResetWithoutChangingAutoMode() {
        SprintState s=new SprintState();
        assertFalse(s.requested(true,ModSettings.SprintMode.TOGGLE));
        assertTrue(s.requested(true,ModSettings.SprintMode.AUTO));
        s.press(); assertTrue(s.requested(true,ModSettings.SprintMode.TOGGLE));
        assertFalse(s.requested(false,ModSettings.SprintMode.TOGGLE));
        s.press(); assertFalse(s.requested(true,ModSettings.SprintMode.TOGGLE));
        s.press(); s.reset(); assertFalse(s.requested(true,ModSettings.SprintMode.TOGGLE));
    }
    @Test public void zoomConvergesIndependentlyOfFrameRateAndAlwaysReleasesOnLostContext() {
        ZoomState a=new ZoomState(),b=new ZoomState();
        a.update(1_000_000_000L,true,false,4,true); b.update(1_000_000_000L,true,false,4,true);
        for(int i=1;i<=20;i++)a.update(1_000_000_000L+i*50_000_000L,true,true,4,true);
        for(int i=1;i<=100;i++)b.update(1_000_000_000L+i*10_000_000L,true,true,4,true);
        assertEquals(4,a.divisor(),.001); assertEquals(a.divisor(),b.divisor(),.001);
        a.update(2_010_000_000L,false,true,4,true); assertEquals(1,a.divisor(),0);
        a.update(2_020_000_000L,true,true,6,false); assertEquals(6,a.divisor(),0);
        a.update(2_030_000_000L,true,false,6,false); assertEquals(1,a.divisor(),0);
    }
    @Test public void crosshairRejectsOutOfRangeGeometry() {
        try { CrosshairStyle.defaults().withThickness(0); fail(); } catch(IllegalArgumentException expected) { }
        try { CrosshairStyle.defaults().withSize(13); fail(); } catch(IllegalArgumentException expected) { }
        try { CrosshairStyle.defaults().withGap(-1); fail(); } catch(IllegalArgumentException expected) { }
        assertEquals(3,CrosshairStyle.Shape.values().length);
    }
}
