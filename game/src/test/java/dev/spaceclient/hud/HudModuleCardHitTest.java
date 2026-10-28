package dev.spaceclient.hud;

import org.junit.Test;
import static org.junit.Assert.*;

public class HudModuleCardHitTest {
    @Test public void toggleRegionIncludesBadgeButNeverLeavesItsCard() {
        for (int[] size : new int[][] {{320, 240}, {427, 240}, {640, 360}, {960, 540}}) {
            HudModulesLayout layout = new HudModulesLayout(size[0], size[1]);
            for (int i = 0; i < HudModulePages.PAGE_SIZE; i++) {
                int x = layout.cardX(i), y = layout.cardY(i);
                int w = layout.cardWidth, h = layout.cardHeight;
                // Badge is x+w-36..x+w-8, y+6+inset..y+20+inset.
                int badgeY = y + 6 + (h - 36) / 2;
                assertTrue(HudModulesScreen.isToggleHit(x + w - 36, badgeY, x, y, w, h));
                assertTrue(HudModulesScreen.isToggleHit(x + w - 9, badgeY + 13, x, y, w, h));
                assertTrue(HudModulesScreen.isToggleHit(x + w - 40, badgeY - 4, x, y, w, h));
                assertTrue(HudModulesScreen.isToggleHit(x + w - 5, badgeY + 17, x, y, w, h));
                assertFalse(HudModulesScreen.isToggleHit(x + w - 41, badgeY, x, y, w, h));
                assertFalse(HudModulesScreen.isToggleHit(x + w - 4, badgeY, x, y, w, h));
                assertFalse(HudModulesScreen.isToggleHit(x + w - 20, badgeY - 5, x, y, w, h));
                assertFalse(HudModulesScreen.isToggleHit(x + w - 20, badgeY + 18, x, y, w, h));
                assertFalse(HudModulesScreen.isToggleHit(x + w - 20, y + h - 1, x, y, w, h));
            }
        }
    }
}
