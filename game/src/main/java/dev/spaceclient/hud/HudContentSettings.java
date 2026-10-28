package dev.spaceclient.hud;

/** Module-specific display formats, stored independently in each HUD profile. */
public final class HudContentSettings {
    public enum ClockFormat { H24, H12 }
    public enum CpsMode { BOTH, LEFT, RIGHT }
    public final int coordinatesPrecision;
    public final ClockFormat clockFormat;
    public final boolean clockSeconds;
    public final boolean fpsLabel;
    public final CpsMode cpsMode;

    public HudContentSettings(int coordinatesPrecision, ClockFormat clockFormat, boolean clockSeconds) {
        this(coordinatesPrecision, clockFormat, clockSeconds, true, CpsMode.BOTH);
    }
    public HudContentSettings(int coordinatesPrecision, ClockFormat clockFormat, boolean clockSeconds,
                              boolean fpsLabel, CpsMode cpsMode) {
        if (coordinatesPrecision < 0 || coordinatesPrecision > 2 || clockFormat == null || cpsMode == null)
            throw new IllegalArgumentException("Invalid HUD content settings");
        this.coordinatesPrecision = coordinatesPrecision;
        this.clockFormat = clockFormat;
        this.clockSeconds = clockSeconds;
        this.fpsLabel = fpsLabel;
        this.cpsMode = cpsMode;
    }
    public static HudContentSettings defaults() { return new HudContentSettings(1, ClockFormat.H24, true); }
    public HudContentSettings withCoordinatesPrecision(int value) {
        return new HudContentSettings(value, clockFormat, clockSeconds, fpsLabel, cpsMode);
    }
    public HudContentSettings withClockFormat(ClockFormat value) {
        return new HudContentSettings(coordinatesPrecision, value, clockSeconds, fpsLabel, cpsMode);
    }
    public HudContentSettings withClockSeconds(boolean value) {
        return new HudContentSettings(coordinatesPrecision, clockFormat, value, fpsLabel, cpsMode);
    }
    public HudContentSettings withFpsLabel(boolean value) {
        return new HudContentSettings(coordinatesPrecision, clockFormat, clockSeconds, value, cpsMode);
    }
    public HudContentSettings withCpsMode(CpsMode value) {
        return new HudContentSettings(coordinatesPrecision, clockFormat, clockSeconds, fpsLabel, value);
    }
}
