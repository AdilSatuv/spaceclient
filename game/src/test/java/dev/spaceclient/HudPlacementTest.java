package dev.spaceclient;

import dev.spaceclient.hud.HudPlacement;
import dev.spaceclient.hud.HudPlacement.Horizontal;
import dev.spaceclient.hud.HudPlacement.Vertical;
import dev.spaceclient.hud.HudEditorLayout;
import org.junit.Test;
import static org.junit.Assert.*;

public class HudPlacementTest {
    @Test public void selectorAndProfileControlsFitWithoutOverlapAtSupportedWidths() {
        for (int width : new int[] {320, 321, 360, 427, 640, 960}) {
            int profile = HudEditorLayout.profileWidth(width);
            int secondProfileEnd = 18 + 2 * profile;
            int previous = HudEditorLayout.previousModuleX(width);
            int title = HudEditorLayout.moduleTitleX(width);
            int next = HudEditorLayout.nextModuleX(width);
            int done = HudEditorLayout.doneX(width);
            assertTrue(secondProfileEnd <= previous);
            assertTrue(previous + HudEditorLayout.NAV_WIDTH <= title);
            assertTrue(title + HudEditorLayout.TITLE_WIDTH <= next);
            assertTrue(next + HudEditorLayout.NAV_WIDTH <= done);
            assertTrue(done + 54 <= width - 8);
        }
    }
    @Test public void moduleSelectionWrapsBothWaysAtTheActualModuleCount() {
        assertEquals(8, HudEditorLayout.nextModuleIndex(0, -1, 9));
        assertEquals(0, HudEditorLayout.nextModuleIndex(8, 1, 9));
        assertEquals(5, HudEditorLayout.nextModuleIndex(6, -1, 9));
        assertThrows(IllegalArgumentException.class, () -> HudEditorLayout.nextModuleIndex(0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> HudEditorLayout.nextModuleIndex(9, 1, 9));
    }
    @Test public void toolbarNeverObscuresTheSelectedWidgetIncludingItsFocusBorder() {
        for (int height : new int[] {240, 360, 540}) for (double scale : new double[] {0.5, 1, 3}) {
            for (int y = 0; y <= height; y++) {
                HudPlacement.Bounds b = new HudPlacement(Horizontal.LEFT, Vertical.TOP, 0, y, scale)
                    .resolve(427, height, 100, 10);
                int toolbar = HudEditorLayout.toolbarY(height, b);
                assertTrue(toolbar >= 0 && toolbar + HudEditorLayout.HEIGHT <= height);
                assertTrue(b.y + b.height + 2 <= toolbar || b.y - 2 >= toolbar + HudEditorLayout.HEIGHT);
            }
        }
    }
    @Test public void everyAnchorKeepsEvenOversizedWidgetsInsideSmallAndLargeViewports() {
        for (Horizontal hx : Horizontal.values()) for (Vertical vy : Vertical.values())
            for (int[] viewport : new int[][] {{1, 1}, {427, 240}, {640, 360}, {960, 540}}) {
                HudPlacement placement = new HudPlacement(hx, vy, 16000, 16000, 3);
                HudPlacement.Bounds b = placement.resolve(viewport[0], viewport[1], 500, 10);
                assertTrue(b.x >= 0 && b.y >= 0);
                assertTrue(b.x + b.width <= viewport[0] + 0.00001);
                assertTrue(b.y + b.height <= viewport[1] + 0.00001);
                assertTrue(b.scale > 0 && b.scale <= 3);
                assertEquals(16000, placement.x, 0); // Fitting must not destroy the preferred position.
            }
    }
    @Test public void rightBottomDistancesAndPreferredScaleSurviveResolutionChanges() {
        HudPlacement placement = new HudPlacement(Horizontal.RIGHT, Vertical.BOTTOM, 8, 12, 2);
        HudPlacement.Bounds large = placement.resolve(960, 540, 100, 10);
        assertEquals(752, large.x, 0);
        assertEquals(508, large.y, 0);
        placement.resolve(40, 20, 100, 10);
        HudPlacement.Bounds restored = placement.resolve(960, 540, 100, 10);
        assertEquals(large.x, restored.x, 0);
        assertEquals(large.scale, restored.scale, 0);
    }
    @Test public void draggingSnapsToAllEdgesButNudgingCanLeaveAnEdgeOneUnitAtATime() {
        HudPlacement placement = new HudPlacement(Horizontal.LEFT, Vertical.TOP, 0, 0, 1);
        HudPlacement snapped = placement.at(4, 5, 427, 240, 100, 10);
        assertEquals(0, snapped.x, 0);
        assertEquals(0, snapped.y, 0);
        HudPlacement right = placement.at(325, 228, 427, 240, 100, 10);
        assertEquals(Horizontal.RIGHT, right.horizontal);
        assertEquals(Vertical.BOTTOM, right.vertical);
        assertEquals(0, right.x, 0);
        assertEquals(0, right.y, 0);
        HudPlacement nudged = placement.at(1, 1, 427, 240, 100, 10, false);
        assertEquals(1, nudged.resolve(427, 240, 100, 10).x, 0);
        assertEquals(1, nudged.resolve(427, 240, 100, 10).y, 0);
    }
    @Test public void changingAnchorsPreservesTheCurrentVisiblePosition() {
        HudPlacement placement = new HudPlacement(Horizontal.LEFT, Vertical.TOP, 70, 80, 1.5);
        for (Horizontal hx : Horizontal.values()) for (Vertical vy : Vertical.values()) {
            HudPlacement anchored = placement.anchored(hx, vy, 640, 360, 100, 10);
            HudPlacement.Bounds b = anchored.resolve(640, 360, 100, 10);
            assertEquals(70, b.x, 0.00001);
            assertEquals(80, b.y, 0.00001);
        }
    }
    @Test public void nonFiniteOutOfRangeAndMissingCoordinatesCannotEnterTheModel() {
        for (double value : new double[] { Double.NaN, Double.POSITIVE_INFINITY, -1, 16385 }) {
            try { new HudPlacement(Horizontal.LEFT, Vertical.TOP, value, 0, 1); fail(); }
            catch (IllegalArgumentException expected) { }
        }
        for (double value : new double[] { Double.NaN, Double.NEGATIVE_INFINITY, 0, 0.49, 3.01 }) {
            try { new HudPlacement(Horizontal.LEFT, Vertical.TOP, 0, 0, value); fail(); }
            catch (IllegalArgumentException expected) { }
        }
    }
}
