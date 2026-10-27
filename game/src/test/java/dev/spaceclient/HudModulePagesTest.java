package dev.spaceclient;

import dev.spaceclient.hud.HudModulePages;
import org.junit.Test;
import static org.junit.Assert.*;

public class HudModulePagesTest {
    @Test public void fourCardsPerPageAndStableAbsoluteIndexes() {
        assertEquals(1, HudModulePages.count(0));
        assertEquals(1, HudModulePages.count(4));
        assertEquals(2, HudModulePages.count(5));
        assertEquals(3, HudModulePages.count(9));
        assertEquals(0, HudModulePages.first(0, 5));
        assertEquals(4, HudModulePages.end(0, 5));
        assertEquals(4, HudModulePages.first(1, 5));
        assertEquals(5, HudModulePages.end(1, 5));
        assertEquals(0, HudModulePages.forIndex(3, 5));
        assertEquals(1, HudModulePages.forIndex(4, 5));
    }

    @Test public void filteringClampsPageAndNavigationWraps() {
        assertEquals(0, HudModulePages.clamp(2, 2));
        assertEquals(0, HudModulePages.clamp(1, 0));
        assertEquals(1, HudModulePages.move(0, -1, 5));
        assertEquals(0, HudModulePages.move(1, 1, 5));
        assertEquals(0, HudModulePages.move(1, 1, 3));
    }

    @Test public void invalidIndexesAndCountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> HudModulePages.count(-1));
        assertThrows(IllegalArgumentException.class, () -> HudModulePages.forIndex(0, 0));
        assertThrows(IllegalArgumentException.class, () -> HudModulePages.forIndex(5, 5));
    }
}
