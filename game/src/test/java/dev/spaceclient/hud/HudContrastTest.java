package dev.spaceclient.hud;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class HudContrastTest {
    @Test public void liveTextRemainsReadableOverWhiteTerrain() {
        // GL blends the translucent backdrop with the framebuffer before text is drawn.
        // White is the worst background for these light text colors.
        double[] backdrop = new double[3];
        for (int i = 0; i < 3; i++) {
            int shift = 16 - 8 * i;
            int channel = (HudRenderer.BACKDROP_RGB >> shift) & 0xFF;
            backdrop[i] = (channel * HudRenderer.BACKDROP_ALPHA + 255.0 * (255 - HudRenderer.BACKDROP_ALPHA)) / 255.0;
        }
        assertTrue("white HUD text must meet 4.5:1", contrast(HudRenderer.HUD_TEXT, backdrop) >= 4.5);
        assertTrue("amber HUD text must meet 4.5:1", contrast(HudRenderer.HUD_ACCENT, backdrop) >= 4.5);
    }

    private static double contrast(int foreground, double[] background) {
        double foregroundLuminance = luminance(new double[] {
            (foreground >> 16) & 0xFF, (foreground >> 8) & 0xFF, foreground & 0xFF
        });
        double backgroundLuminance = luminance(background);
        return (Math.max(foregroundLuminance, backgroundLuminance) + 0.05)
            / (Math.min(foregroundLuminance, backgroundLuminance) + 0.05);
    }

    private static double luminance(double[] rgb) {
        double[] weights = {0.2126, 0.7152, 0.0722};
        double result = 0;
        for (int i = 0; i < 3; i++) {
            double c = rgb[i] / 255.0;
            result += weights[i] * (c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4));
        }
        return result;
    }
}
