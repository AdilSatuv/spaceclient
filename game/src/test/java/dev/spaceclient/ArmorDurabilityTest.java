package dev.spaceclient;

import dev.spaceclient.hud.ArmorDurability;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ArmorDurabilityTest {
    @Test public void distinguishesEmptyNonDamageableAndInvalid() {
        assertEquals("-", ArmorDurability.value(false, false, 0, 0));
        assertEquals("n/a", ArmorDurability.value(true, false, 0, 0));
        assertEquals("n/a", ArmorDurability.value(true, false, 8, 100));
        assertEquals("?", ArmorDurability.value(true, true, 0, 0));
        assertEquals("?", ArmorDurability.value(true, true, -1, 100));
        assertEquals("?", ArmorDurability.value(true, true, 101, 100));
    }

    @Test public void reportsRemainingRatherThanDamageAndNeverRoundsLiveItemsToZero() {
        assertEquals("100%", ArmorDurability.value(true, true, 0, 200));
        assertEquals("50%", ArmorDurability.value(true, true, 100, 200));
        assertEquals("1%", ArmorDurability.value(true, true, 1560, 1561));
        assertEquals("0%", ArmorDurability.value(true, true, 1561, 1561));
        assertEquals("1%", ArmorDurability.value(true, true, Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
    }
}
