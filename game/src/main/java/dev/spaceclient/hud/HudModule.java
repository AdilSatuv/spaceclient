package dev.spaceclient.hud;

import dev.spaceclient.HudConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Stable module identities. No Minecraft, rendering or input dependencies. */
public enum HudModule {
    FPS("fps", "FPS", "Frame rate", "frames performance кадры фпс"),
    COORDINATES("coordinates", "Coordinates", "Position in your world", "xyz position координаты позиция"),
    KEYSTROKES("keystrokes", "Keystrokes", "Movement key activity", "wasd movement keyboard клавиши движение"),
    CPS("cps", "CPS", "Left and right clicks / second", "clicks mouse клики мышь кпс"),
    COMPASS("compass", "Compass", "Direction you are facing", "direction facing heading компас направление стороны света");

    private final String id;
    private final String title;
    private final String description;
    private final String searchText;

    HudModule(String id, String title, String description, String aliases) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.searchText = (id + " " + title + " " + description + " " + aliases).toLowerCase(Locale.ROOT);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }

    public boolean isEnabled(HudConfig config) {
        switch (this) {
            case FPS: return config.active().fps;
            case COORDINATES: return config.active().coordinates;
            case KEYSTROKES: return config.active().keystrokes;
            case CPS: return config.active().cps;
            case COMPASS: return config.active().compass;
            default: throw new AssertionError(this);
        }
    }

    public void setEnabled(HudConfig config, boolean enabled) {
        switch (this) {
            case FPS: config.active().fps = enabled; break;
            case COORDINATES: config.active().coordinates = enabled; break;
            case KEYSTROKES: config.active().keystrokes = enabled; break;
            case CPS: config.active().cps = enabled; break;
            case COMPASS: config.active().compass = enabled; break;
            default: throw new AssertionError(this);
        }
    }

    public static List<HudModule> search(String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        List<HudModule> result = new ArrayList<>();
        for (HudModule module : values()) {
            if (module.searchText.contains(normalized)) result.add(module);
        }
        return Collections.unmodifiableList(result);
    }
}
