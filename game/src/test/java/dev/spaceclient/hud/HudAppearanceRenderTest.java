package dev.spaceclient.hud;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** Renderer maps saved styles consistently in the world and editor. */
public class HudAppearanceRenderTest {
    @Test public void defaultAppearancePreservesLegacyModuleColorsAndBackdrop() {
        HudAppearance a = HudAppearance.defaults();
        assertEquals(HudRenderer.HUD_TEXT, HudRenderer.color(HudModule.FPS, a));
        assertEquals(HudRenderer.HUD_ACCENT, HudRenderer.color(HudModule.KEYSTROKES, a));
        assertEquals(208, HudRenderer.backdropAlpha(a));
    }
    @Test public void explicitColorsAndBackdropModesApplyToEveryModule() {
        for (HudModule module : HudModule.values()) {
            HudAppearance a = HudAppearance.defaults();
            assertEquals(HudRenderer.HUD_TEXT, HudRenderer.color(module, a.withColor(HudAppearance.Color.WHITE)));
            assertEquals(HudRenderer.HUD_ACCENT, HudRenderer.color(module, a.withColor(HudAppearance.Color.GOLD)));
            assertEquals(HudRenderer.HUD_AQUA, HudRenderer.color(module, a.withColor(HudAppearance.Color.AQUA)));
            assertEquals(176, HudRenderer.backdropAlpha(a.withBackdrop(HudAppearance.Backdrop.SOFT)));
            assertEquals(0, HudRenderer.backdropAlpha(a.withBackdrop(HudAppearance.Backdrop.OFF)));
        }
    }
}
