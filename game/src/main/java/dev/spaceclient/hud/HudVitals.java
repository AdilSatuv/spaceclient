package dev.spaceclient.hud;

import java.util.Locale;

/** Vanilla health points and food level; does not include absorption or saturation. */
public final class HudVitals {
    private HudVitals() { }

    public static String health(float current, float maximum) {
        if (!Float.isFinite(current) || !Float.isFinite(maximum) || current < 0 || maximum <= 0)
            return "Health -- / --";
        return String.format(Locale.ROOT, "Health %.1f / %.1f", current, maximum);
    }

    public static String hunger(Integer foodLevel) {
        if (foodLevel == null || foodLevel < 0 || foodLevel > 20) return "Hunger -- / 20";
        return "Hunger " + foodLevel + " / 20";
    }
}
