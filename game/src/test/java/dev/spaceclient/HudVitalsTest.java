package dev.spaceclient;

import dev.spaceclient.hud.HudVitals;
import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HudVitalsTest {
    @Test public void healthUsesHealthPointsAndPreservesFractions() {
        assertEquals("Health 19.5 / 20.0", HudVitals.health(19.5f, 20));
        assertEquals("Health 0.0 / 40.0", HudVitals.health(0, 40));
        assertEquals("Health 25.0 / 20.0", HudVitals.health(25, 20));
    }

    @Test public void invalidOrUnavailableHealthIsNotReportedAsZero() {
        assertEquals("Health -- / --", HudVitals.health(Float.NaN, 20));
        assertEquals("Health -- / --", HudVitals.health(5, Float.POSITIVE_INFINITY));
        assertEquals("Health -- / --", HudVitals.health(-1, 20));
        assertEquals("Health -- / --", HudVitals.health(5, 0));
    }

    @Test public void hungerUsesVanillaFoodLevelAndHandlesUnavailableData() {
        assertEquals("Hunger 20 / 20", HudVitals.hunger(20));
        assertEquals("Hunger 0 / 20", HudVitals.hunger(0));
        assertEquals("Hunger -- / 20", HudVitals.hunger(null));
        assertEquals("Hunger -- / 20", HudVitals.hunger(-1));
        assertEquals("Hunger -- / 20", HudVitals.hunger(21));
    }

    @Test public void decimalOutputDoesNotDependOnSystemLocale() {
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("Health 5.5 / 20.0", HudVitals.health(5.5f, 20));
        } finally { Locale.setDefault(before); }
    }
}
