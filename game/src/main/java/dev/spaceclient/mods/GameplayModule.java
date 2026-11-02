package dev.spaceclient.mods;

import java.util.Locale;

/** Controls and rendering features are independent of movable HUD widgets. */
public enum GameplayModule {
    TOGGLE_SPRINT("ToggleSprint", "AutoSprint or press-to-toggle sprint", "autosprint toggle sprint бег спринт"),
    FREE_LOOK("FreeLook", "Look around without turning your player", "freelook camera свободная камера обзор"),
    ZOOM("Zoom", "Hold a key for smooth camera zoom", "zoom приближение зум"),
    CROSSHAIR("Crosshair", "Customize shape, size, gap and color", "crosshair aim прицел");
    public final String title, description;
    private final String search;
    GameplayModule(String title, String description, String aliases) {
        this.title = title; this.description = description;
        search = (title + " " + description + " " + aliases).toLowerCase(Locale.ROOT);
    }
    public boolean matches(String query) { return search.contains(query.trim().toLowerCase(Locale.ROOT)); }
    public boolean enabled(ModSettings s) {
        switch (this) {
            case TOGGLE_SPRINT: return s.autoSprint(); case FREE_LOOK: return s.freeLook();
            case ZOOM: return s.zoom(); case CROSSHAIR: return s.crosshair();
            default: throw new AssertionError(this);
        }
    }
    public boolean toggle(ModSettings s) {
        switch (this) {
            case TOGGLE_SPRINT: return s.toggleAutoSprint(); case FREE_LOOK: return s.toggleFreeLook();
            case ZOOM: return s.toggleZoom(); case CROSSHAIR: return s.toggleCrosshair();
            default: throw new AssertionError(this);
        }
    }
}
