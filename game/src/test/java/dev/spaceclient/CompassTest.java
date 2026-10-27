package dev.spaceclient;

import dev.spaceclient.hud.Compass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CompassTest {
    @Test public void mapsMinecraftYawToEightDirections() {
        assertEquals("S", Compass.direction(0));
        assertEquals("SW", Compass.direction(45));
        assertEquals("W", Compass.direction(90));
        assertEquals("NW", Compass.direction(135));
        assertEquals("N", Compass.direction(180));
        assertEquals("NE", Compass.direction(-135));
        assertEquals("E", Compass.direction(-90));
        assertEquals("SE", Compass.direction(-45));
    }

    @Test public void handlesBoundariesWrappedAnglesAndInvalidValues() {
        assertEquals("S", Compass.direction(22.499));
        assertEquals("SW", Compass.direction(22.5));
        assertEquals("S", Compass.direction(-22.5));
        assertEquals("SE", Compass.direction(-22.501));
        assertEquals("N", Compass.direction(540));
        assertEquals("E", Compass.direction(-450));
        assertEquals("--", Compass.direction(Double.NaN));
        assertEquals("--", Compass.direction(Double.POSITIVE_INFINITY));
    }
}
