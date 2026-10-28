package dev.spaceclient;

import dev.spaceclient.hud.HudMetrics;
import dev.spaceclient.hud.HudContentSettings.ClockFormat;
import dev.spaceclient.hud.HudContentSettings.CpsMode;
import dev.spaceclient.hud.HudPlacement;
import java.util.Locale;
import java.time.LocalTime;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class HudMetricsTest {
    @Test public void clockIsLocalAndZeroPadded() {
        assertEquals("00:02:03", HudMetrics.clock(LocalTime.of(0, 2, 3)));
        assertEquals("23:59:59", HudMetrics.clock(LocalTime.of(23, 59, 59)));
        assertEquals("--:--:--", HudMetrics.clock(null));
    }

    @Test public void clockFormatsMidnightNoonAndOptionalSeconds() {
        assertEquals("00:00", HudMetrics.clock(LocalTime.MIDNIGHT, ClockFormat.H24, false));
        assertEquals("12:00 AM", HudMetrics.clock(LocalTime.MIDNIGHT, ClockFormat.H12, false));
        assertEquals("12:00 PM", HudMetrics.clock(LocalTime.NOON, ClockFormat.H12, false));
        assertEquals("01:04:09 PM", HudMetrics.clock(LocalTime.of(13, 4, 9), ClockFormat.H12, true));
        assertEquals("23:59:59", HudMetrics.clock(LocalTime.of(23, 59, 59), ClockFormat.H24, true));
        assertEquals("--:-- --", HudMetrics.clock(null, ClockFormat.H12, false));
        assertEquals("--:--:--", HudMetrics.clock(null, ClockFormat.H24, true));
        assertThrows(IllegalArgumentException.class, () -> HudMetrics.clock(LocalTime.NOON, null, true));
    }

    @Test public void coordinatesRespectPrecisionSignsAndLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            assertEquals("XYZ -12 / 64 / 9", HudMetrics.coordinates(-12.34, 64.4, 8.76, 0));
            assertEquals("XYZ -12.3 / 64.4 / 8.8", HudMetrics.coordinates(-12.34, 64.4, 8.76, 1));
            assertEquals("XYZ -12.34 / 64.40 / 8.76", HudMetrics.coordinates(-12.34, 64.4, 8.76, 2));
            assertEquals("XYZ --", HudMetrics.coordinates(Double.NaN, 64, 0, 1));
            assertThrows(IllegalArgumentException.class, () -> HudMetrics.coordinates(0, 0, 0, 3));
        } finally { Locale.setDefault(previous); }
    }

    @Test public void rightAnchorRetainsDistanceAsFormattedTextWidthChanges() {
        HudPlacement right = new HudPlacement(HudPlacement.Horizontal.RIGHT, HudPlacement.Vertical.TOP, 8, 12, 1);
        String shortText = HudMetrics.coordinates(-12.34, 64.4, 8.76, 0);
        String longText = HudMetrics.coordinates(-12.34, 64.4, 8.76, 2);
        // The renderer supplies the current measured width; its anchor must remain stable as text changes.
        for (String text : new String[] { shortText, longText }) {
            HudPlacement.Bounds b = right.resolve(640, 360, text.length() * 6, 10);
            assertEquals(8, 640 - b.x - b.width, 0.00001);
        }
    }

    @Test public void fpsAndCpsModesKeepLegacyDefaultsAndDistinctSides() {
        assertEquals("144 FPS", HudMetrics.fps(144, true));
        assertEquals("144", HudMetrics.fps(144, false));
        assertEquals("CPS 7 L / 3 R", HudMetrics.cps(7, 3, CpsMode.BOTH));
        assertEquals("CPS 7 L", HudMetrics.cps(7, 3, CpsMode.LEFT));
        assertEquals("CPS 3 R", HudMetrics.cps(7, 3, CpsMode.RIGHT));
        assertEquals("CPS 0 L / 0 R", HudMetrics.cps(0, 0, CpsMode.BOTH));
        assertThrows(IllegalArgumentException.class, () -> HudMetrics.cps(7, 3, null));
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
