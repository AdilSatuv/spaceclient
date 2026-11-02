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
    public final EquipmentContent equipment;
    public final boolean inventoryHotbar, inventoryOverlays;

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
        this(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,packLabel,packNames,keyLayout,sprintDisplay,EquipmentContent.defaults());
    }
    public HudContentSettings(int coordinatesPrecision, ClockFormat clockFormat, boolean clockSeconds,
                              boolean fpsLabel, CpsMode cpsMode, boolean keystrokesJump, boolean keystrokesMouse,
                              boolean pingLabel, boolean biomeLabel, boolean packLabel, int packNames,
                              KeyLayout keyLayout, SprintDisplay sprintDisplay, EquipmentContent equipment) {
        this(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,packLabel,packNames,keyLayout,sprintDisplay,equipment,false,true);
    }
    public HudContentSettings(int coordinatesPrecision, ClockFormat clockFormat, boolean clockSeconds,
                              boolean fpsLabel, CpsMode cpsMode, boolean keystrokesJump, boolean keystrokesMouse,
                              boolean pingLabel, boolean biomeLabel, boolean packLabel, int packNames,
                              KeyLayout keyLayout, SprintDisplay sprintDisplay, EquipmentContent equipment,
                              boolean inventoryHotbar, boolean inventoryOverlays) {
        this.inventoryHotbar=inventoryHotbar;this.inventoryOverlays=inventoryOverlays;
        if(equipment==null)throw new IllegalArgumentException("Missing equipment formats");this.equipment=equipment;
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
    public HudContentSettings withInventoryHotbar(boolean value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,packLabel,packNames,keyLayout,sprintDisplay,equipment,value,inventoryOverlays);
    }
    public HudContentSettings withInventoryOverlays(boolean value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,packLabel,packNames,keyLayout,sprintDisplay,equipment,inventoryHotbar,value);
    }
    public HudContentSettings withCoordinatesPrecision(int value) {
        return new HudContentSettings(value, clockFormat, clockSeconds, fpsLabel, cpsMode, keystrokesJump, keystrokesMouse, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withClockFormat(ClockFormat value) {
        return new HudContentSettings(coordinatesPrecision, value, clockSeconds, fpsLabel, cpsMode, keystrokesJump, keystrokesMouse, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withClockSeconds(boolean value) {
        return new HudContentSettings(coordinatesPrecision, clockFormat, value, fpsLabel, cpsMode, keystrokesJump, keystrokesMouse, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withFpsLabel(boolean value) {
        return new HudContentSettings(coordinatesPrecision, clockFormat, clockSeconds, value, cpsMode, keystrokesJump, keystrokesMouse, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withCpsMode(CpsMode value) {
        return new HudContentSettings(coordinatesPrecision, clockFormat, clockSeconds, fpsLabel, value, keystrokesJump, keystrokesMouse, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withKeystrokesJump(boolean value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,value,keystrokesMouse, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withKeystrokesMouse(boolean value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,value, pingLabel, biomeLabel, packLabel, packNames, keyLayout, sprintDisplay, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withPingLabel(boolean value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,value,biomeLabel,packLabel,packNames, keyLayout, sprintDisplay, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withBiomeLabel(boolean value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,value,packLabel,packNames, keyLayout, sprintDisplay, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withPackLabel(boolean value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,value,packNames, keyLayout, sprintDisplay, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withPackNames(int value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,packLabel,value, keyLayout, sprintDisplay, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withKeyLayout(KeyLayout value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,packLabel,packNames,value,sprintDisplay, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withSprintDisplay(SprintDisplay value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,packLabel,packNames,keyLayout,value, equipment, inventoryHotbar, inventoryOverlays);
    }
    public HudContentSettings withEquipment(EquipmentContent value) {
        return new HudContentSettings(coordinatesPrecision,clockFormat,clockSeconds,fpsLabel,cpsMode,keystrokesJump,keystrokesMouse,pingLabel,biomeLabel,packLabel,packNames,keyLayout,sprintDisplay,value, inventoryHotbar, inventoryOverlays);
    }
}
