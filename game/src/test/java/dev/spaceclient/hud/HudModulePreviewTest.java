package dev.spaceclient.hud;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HudModulePreviewTest {
    @Test public void previewTracksScaleUntilItMustFitItsShortRow() {
        assertEquals(0.5, HudModuleSettingsScreen.previewScale(0.5), 0);
        assertEquals(1.0, HudModuleSettingsScreen.previewScale(1.0), 0);
        assertEquals(1.4, HudModuleSettingsScreen.previewScale(1.4), 0);
        assertEquals(1.4, HudModuleSettingsScreen.previewScale(3.0), 0);
    }
}
