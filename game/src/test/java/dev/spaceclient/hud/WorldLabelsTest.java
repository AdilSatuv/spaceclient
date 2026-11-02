package dev.spaceclient.hud;
import org.junit.Test;
import static org.junit.Assert.*;
public class WorldLabelsTest {
    @Test public void dayCounterUsesWorldDaysStartingAtOneWithoutOverflow() {
        assertEquals("Day: --",WorldLabels.day(null));assertEquals("Day: --",WorldLabels.day(-1L));
        assertEquals("Day: 1",WorldLabels.day(0L));assertEquals("Day: 1",WorldLabels.day(23999L));
        assertEquals("Day: 2",WorldLabels.day(24000L));assertEquals("Day: 3",WorldLabels.day(48000L));
        assertEquals("Day: 384307168202283",WorldLabels.day(Long.MAX_VALUE));assertEquals("Day: --",WorldHud.day(null));
    }
    @Test public void skyAndBlockLightKeepUnknownChannelsDistinctFromDarkness() {
        assertEquals("Light S:15 B:0",WorldLabels.light(15,0));assertEquals("Light S:0 B:0",WorldLabels.light(0,0));
        assertEquals("Light S:-- B:7",WorldLabels.light(null,7));assertEquals("Light S:8 B:--",WorldLabels.light(8,16));
        assertEquals("Light S:-- B:--",WorldLabels.light(-1,null));assertEquals("Light S:-- B:--",WorldHud.light(null));
    }
}
