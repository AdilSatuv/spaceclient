package dev.spaceclient.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Pure display formatting. Speed is horizontal displacement per tick at nominal 20 TPS. */
public final class HudMetrics {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
    private static final DateTimeFormatter CLOCK_MINUTES = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final DateTimeFormatter CLOCK_12 = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ROOT);
    private static final DateTimeFormatter CLOCK_12_MINUTES = DateTimeFormatter.ofPattern("hh:mm a", Locale.ROOT);
    private HudMetrics() { }

    public static String clock(LocalTime time) {
        return time == null ? "--:--:--" : CLOCK.format(time);
    }

    /** Format a local clock independently of the machine locale. */
    public static String clock(LocalTime time, HudContentSettings.ClockFormat format, boolean seconds) {
        if (format == null) throw new IllegalArgumentException("Missing clock format");
        if (time == null) return (seconds ? "--:--:--" : "--:--")
            + (format == HudContentSettings.ClockFormat.H12 ? " --" : "");
        DateTimeFormatter formatter = format == HudContentSettings.ClockFormat.H12
            ? (seconds ? CLOCK_12 : CLOCK_12_MINUTES) : (seconds ? CLOCK : CLOCK_MINUTES);
        return formatter.format(time);
    }

    /** Preserve the default suffix unless the user requests the numeric value alone. */
    public static String fps(int frames, boolean showLabel) {
        return Integer.toString(frames) + (showLabel ? " FPS" : "");
    }

    /** Display left and/or right click counts without losing their labels. */
    public static String cps(int left, int right, HudContentSettings.CpsMode mode) {
        if (mode == null) throw new IllegalArgumentException("Missing CPS mode");
        switch (mode) {
            case BOTH: return "CPS " + left + " L / " + right + " R";
            case LEFT: return "CPS " + left + " L";
            case RIGHT: return "CPS " + right + " R";
            default: throw new AssertionError(mode);
        }
    }

    /** Decimal precision is 0–2; non-finite input never appears as a coordinate. */
    public static String coordinates(double x, double y, double z, int precision) {
        if (precision < 0 || precision > 2) throw new IllegalArgumentException("Invalid coordinate precision");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return "XYZ --";
        String component = "%." + precision + "f";
        return String.format(Locale.ROOT, "XYZ " + component + " / " + component + " / " + component, x, y, z);
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
