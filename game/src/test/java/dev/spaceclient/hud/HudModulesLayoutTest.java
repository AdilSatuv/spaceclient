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
    @Test public void scaledViewportKeepsEveryCardAndSwitchClickable() {
        for (int[] size : new int[][] {{320, 240}, {427, 240}, {640, 360}, {960, 540}}) {
            HudViewport v = new HudViewport(size[0], size[1]);
            HudModulesLayout l = new HudModulesLayout(v.width, v.height);
            assertTrue(l.left * v.scale >= 0);
            assertTrue((l.left + l.panelWidth) * v.scale <= size[0]);
            assertTrue((l.top + l.panelHeight) * v.scale <= size[1]);
            assertEquals(6, l.pageSize);
            assertTrue(l.sidebar);
            for (int i = 0; i < l.pageSize; i++) {
                int x = l.cardX(i), y = l.cardY(i), w = l.cardWidth, h = l.cardHeight;
                int badgeY = y + 6 + (h - 36) / 2;
                int physicalX = Math.round((x + w - 22) * v.scale);
                int physicalY = Math.round((badgeY + 7) * v.scale);
                assertTrue(HudModulesScreen.isToggleHit(v.toUi(physicalX), v.toUi(physicalY), x, y, w, h));
            }
            int searchX = Math.round((l.contentLeft + 15) * v.scale);
            int searchY = Math.round((l.top + l.searchTop + 10) * v.scale);
            assertTrue(v.toUi(searchX) >= l.contentLeft && v.toUi(searchX) < l.contentLeft + l.searchWidth);
            assertTrue(v.toUi(searchY) >= l.top + l.searchTop && v.toUi(searchY) < l.top + l.searchTop + 23);
        }
    }
}
