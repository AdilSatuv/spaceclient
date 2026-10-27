package dev.spaceclient.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Pure display formatting. Speed is horizontal displacement per tick at nominal 20 TPS. */
public final class HudMetrics {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
    private HudMetrics() { }

    public static String clock(LocalTime time) {
        return time == null ? "--:--:--" : CLOCK.format(time);
    }

    public static String speed(double x, double z, double previousX, double previousZ, boolean paused) {
        if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(previousX) || !Double.isFinite(previousZ))
            return "Speed --";
        if (paused) return "Speed 0.0 b/s";
        double blocksPerSecond = Math.hypot(x - previousX, z - previousZ) * 20.0;
        if (!Double.isFinite(blocksPerSecond)) return "Speed --";
        return String.format(Locale.ROOT, "Speed %.1f b/s", blocksPerSecond);
    }
}
