package dev.spaceclient.hud;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class HudAlignmentTest {
    @Test public void gridAndNearbyEdgesRespectWidgetDimensions() {
        assertArrayEquals(new double[]{16,24},HudAlignment.snap(13,27,20,10,320,240,Collections.emptyList(),true),0);
        assertArrayEquals(new double[]{150,115},HudAlignment.snap(152,116,20,10,320,240,Collections.emptyList(),false),0);
        HudPlacement.Bounds other=new HudPlacement(HudPlacement.Horizontal.LEFT,HudPlacement.Vertical.TOP,80,60,1).resolve(320,240,40,10);
        double[] snap=HudAlignment.snap(81,73,20,10,320,240,Collections.singletonList(other),false);
        assertEquals(80,snap[0],0);assertEquals(70,snap[1],0);
        assertArrayEquals(new double[]{123,93},HudAlignment.snap(123,93,20,10,320,240,Collections.emptyList(),false),0);
    }
}
