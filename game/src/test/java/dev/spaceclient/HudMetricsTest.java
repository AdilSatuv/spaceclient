package dev.spaceclient;

import dev.spaceclient.hud.HudMetrics;
import java.time.LocalTime;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HudMetricsTest {
    @Test public void clockIsLocalAndZeroPadded() {
        assertEquals("00:02:03", HudMetrics.clock(LocalTime.of(0, 2, 3)));
        assertEquals("23:59:59", HudMetrics.clock(LocalTime.of(23, 59, 59)));
        assertEquals("--:--:--", HudMetrics.clock(null));
    }

    @Test public void speedUsesOnlyHorizontalTickDisplacementAtNominalTickRate() {
        assertEquals("Speed 0.0 b/s", HudMetrics.speed(2, 3, 2, 3, false));
        assertEquals("Speed 10.0 b/s", HudMetrics.speed(0.3, -0.4, 0, 0, false));
        assertEquals("Speed 0.0 b/s", HudMetrics.speed(0.3, -0.4, 0, 0, true));
    }

    @Test public void badCoordinatesAndOverflowAreNotReportedAsSpeed() {
        assertEquals("Speed --", HudMetrics.speed(Double.NaN, 0, 0, 0, false));
        assertEquals("Speed --", HudMetrics.speed(Double.POSITIVE_INFINITY, 0, 0, 0, false));
        assertEquals("Speed --", HudMetrics.speed(Double.MAX_VALUE, 0, -Double.MAX_VALUE, 0, false));
        assertEquals("Speed --", HudMetrics.speed(Double.NaN, 0, 0, 0, true));
    }
}
