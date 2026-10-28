package dev.spaceclient.hud;

import org.junit.Test;
import static org.junit.Assert.*;

public class HudModulesLayoutTest {
    @Test public void controlsAndCardsStayInsideAtSupportedGuiSizes() {
        for (int width : new int[] {320, 321, 427, 464, 640, 960}) for (int height : new int[] {240, 270, 360, 540}) {
            HudModulesLayout l = new HudModulesLayout(width, height);
            assertTrue(l.left >= 0 && l.left + l.panelWidth <= width);
            assertTrue(l.top >= 0 && l.top + l.panelHeight <= height);
            assertTrue(l.contentWidth >= 200);
            assertTrue(l.contentLeft >= l.left && l.contentLeft + l.contentWidth <= l.left + l.panelWidth);
            assertTrue(l.cardX(0) + l.cardWidth + 8 <= l.cardX(1));
            assertTrue(l.cardX(1) + l.cardWidth <= l.contentLeft + l.contentWidth);
            assertTrue(l.cardY(0) + l.cardHeight + 6 <= l.cardY(2));
            assertTrue(l.cardY(l.pageSize - 2) + l.cardHeight <= l.footerY() - 4);
            if (l.sidebar) {
                assertEquals(l.contentWidth, l.searchWidth);
                assertTrue(l.profileLeft >= l.left && l.profileLeft + l.profileWidth <= l.contentLeft - 12);
                assertTrue(l.profileTop > l.top + 100 && l.profileTop + 23 <= l.top + l.panelHeight - 54);
            } else {
                assertTrue(l.contentLeft + l.searchWidth + 8 <= l.profileLeft);
                assertTrue(l.profileLeft + l.profileWidth <= l.contentLeft + l.contentWidth);
            }
            assertTrue(l.footerY() + 20 <= l.top + l.panelHeight);
            assertTrue(l.contentLeft + 84 <= l.contentLeft + l.contentWidth - 68);
        }
    }
    @Test public void paginationFitsSixCardsWhenRoomyAndFourWhenCompact() {
        HudModulesLayout compact = new HudModulesLayout(427, 240);
        HudModulesLayout roomy = new HudModulesLayout(640, 360);
        assertEquals(6, compact.pageSize);
        assertEquals(6, roomy.pageSize);
        assertTrue(compact.sidebar);
        assertEquals(2, HudModulePages.count(8, roomy.pageSize));
        assertEquals(6, HudModulePages.first(1, 8, roomy.pageSize));
        assertEquals(8, HudModulePages.end(1, 8, roomy.pageSize));
        assertEquals(1, HudModulePages.forIndex(6, 8, roomy.pageSize));
        assertEquals(0, HudModulePages.clamp(1, 3, roomy.pageSize));
    }
    @Test public void screenshotModulesLeadTheBrowserWithoutLosingOthers() {
        java.util.List<HudModule> all = HudModule.search("");
        assertEquals(java.util.Arrays.asList(HudModule.FPS, HudModule.COORDINATES, HudModule.KEYSTROKES,
            HudModule.CPS, HudModule.COMPASS, HudModule.ARMOR), all.subList(0, 6));
        assertEquals(HudModule.values().length, all.size());
        assertEquals(all.size(), new java.util.HashSet<HudModule>(all).size());
    }
}
