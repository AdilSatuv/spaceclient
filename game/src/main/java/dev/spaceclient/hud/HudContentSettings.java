package dev.spaceclient.hud;

/** Module-specific display formats, stored independently in each HUD profile. */
public final class HudContentSettings {
    public enum ClockFormat { H24, H12 }
    public enum CpsMode { BOTH, LEFT, RIGHT }
    public enum KeyLayout { LINE, BLOCK }
    public enum SprintDisplay { FULL, COMPACT }
    public final int coordinatesPrecision;
    public final ClockFormat clockFormat;
    public final boolean clockSeconds;
    public final boolean fpsLabel;
    public final CpsMode cpsMode;
    public final boolean keystrokesJump, keystrokesMouse;
    public final boolean pingLabel, biomeLabel, packLabel;
    public final int packNames;
    public final KeyLayout keyLayout;
    public final SprintDisplay sprintDisplay;

    public HudContentSettings(int coordinatesPrecision, ClockFormat clockFormat, boolean clockSeconds) {
        this(coordinatesPrecision, clockFormat, clockSeconds, true, CpsMode.BOTH);
    }
    public HudContentSettings(int coordinatesPrecision, ClockFormat clockFormat, boolean clockSeconds,
                              boolean fpsLabel, CpsMode cpsMode) {
        this(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,true,true);
    }
    public HudContentSettings(int coordinatesPrecision, ClockFormat clockFormat, boolean clockSeconds,
                              boolean fpsLabel, CpsMode cpsMode, boolean keystrokesJump, boolean keystrokesMouse) {
        this(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,true,true,true,2);
    }
    public HudContentSettings(int coordinatesPrecision, ClockFormat clockFormat, boolean clockSeconds,
                              boolean fpsLabel, CpsMode cpsMode, boolean keystrokesJump, boolean keystrokesMouse,
                              boolean pingLabel, boolean biomeLabel, boolean packLabel, int packNames) {
        this(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,packLabel,packNames,KeyLayout.LINE,SprintDisplay.FULL);
    }
    public HudContentSettings(int coordinatesPrecision, ClockFormat clockFormat, boolean clockSeconds,
                              boolean fpsLabel, CpsMode cpsMode, boolean keystrokesJump, boolean keystrokesMouse,
                              boolean pingLabel, boolean biomeLabel, boolean packLabel, int packNames,
                              KeyLayout keyLayout, SprintDisplay sprintDisplay) {
        if(keyLayout==null||sprintDisplay==null)throw new IllegalArgumentException("Missing widget format");
        this.keyLayout=keyLayout;this.sprintDisplay=sprintDisplay;
        if (packNames < 1 || packNames > 3) throw new IllegalArgumentException("Invalid pack name count");
        if (coordinatesPrecision < 0 || coordinatesPrecision > 2 || clockFormat == null || cpsMode == null)
            throw new IllegalArgumentException("Invalid HUD content settings");
        this.coordinatesPrecision = coordinatesPrecision;
        this.clockFormat = clockFormat;
        this.clockSeconds = clockSeconds;
        this.fpsLabel = fpsLabel;
        this.cpsMode = cpsMode;
        this.keystrokesJump = keystrokesJump; this.keystrokesMouse = keystrokesMouse;
        this.pingLabel = pingLabel; this.biomeLabel = biomeLabel; this.packLabel = packLabel; this.packNames = packNames;
    }
    public static HudContentSettings defaults() { return new HudContentSettings(1, ClockFormat.H24, true); }
    public HudContentSettings withCoordinatesPrecision(int value) {
        return new HudContentSettings(value, clockFormat, clockSeconds, fpsLabel, cpsMode, keystrokesJump, keystrokesMouse, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay);
    }
    public HudContentSettings withClockFormat(ClockFormat value) {
        return new HudContentSettings(coordinatesPrecision, value, clockSeconds, fpsLabel, cpsMode, keystrokesJump, keystrokesMouse, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay);
    }
    public HudContentSettings withClockSeconds(boolean value) {
        return new HudContentSettings(coordinatesPrecision, clockFormat, value, fpsLabel, cpsMode, keystrokesJump, keystrokesMouse, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay);
    }
    public HudContentSettings withFpsLabel(boolean value) {
        return new HudContentSettings(coordinatesPrecision, clockFormat, clockSeconds, value, cpsMode, keystrokesJump, keystrokesMouse, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay);
    }
    public HudContentSettings withCpsMode(CpsMode value) {
        return new HudContentSettings(coordinatesPrecision, clockFormat, clockSeconds, fpsLabel, value, keystrokesJump, keystrokesMouse, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay);
    }
    public HudContentSettings withKeystrokesJump(boolean value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,value,keystrokesMouse, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay);
    }
    public HudContentSettings withKeystrokesMouse(boolean value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,value, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay);
    }
    public HudContentSettings withPingLabel(boolean value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,value,biomeLabel,packLabel,packNames, keyLayout, sprintDisplay);
    }
    public HudContentSettings withBiomeLabel(boolean value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,value,packLabel,packNames, keyLayout, sprintDisplay);
    }
    public HudContentSettings withPackLabel(boolean value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,value,packNames, keyLayout, sprintDisplay);
    }
    public HudContentSettings withPackNames(int value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,packLabel,value, keyLayout, sprintDisplay);
    }
    public HudContentSettings withKeyLayout(KeyLayout value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,packLabel,packNames,value,sprintDisplay);
    }
    public HudContentSettings withSprintDisplay(SprintDisplay value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,packLabel,packNames,keyLayout,value);
    }
}
