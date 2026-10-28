package dev.spaceclient.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/** Navigation filters existing HUD modules; Settings opens the native options screen. */
enum HudSection {
    HOME("Home", "Your Space HUD modules", EnumSet.allOf(HudModule.class)),
    COMBAT("Combat", "Combat-related HUD indicators", EnumSet.of(HudModule.CPS, HudModule.KEYSTROKES, HudModule.HEALTH, HudModule.ARMOR, HudModule.POTION_EFFECTS)),
    VISUALS("Visuals", "Visual information on your HUD", EnumSet.of(HudModule.FPS, HudModule.COORDINATES, HudModule.COMPASS, HudModule.CLOCK, HudModule.POTION_EFFECTS)),
    HUD("HUD", "Customize your in-game overlay", EnumSet.allOf(HudModule.class)),
    PLAYER("Player", "Player HUD indicators", EnumSet.of(HudModule.KEYSTROKES, HudModule.SPEED, HudModule.HEALTH, HudModule.HUNGER, HudModule.ARMOR, HudModule.POTION_EFFECTS)),
    WORLD("World", "World position and direction", EnumSet.of(HudModule.COORDINATES, HudModule.COMPASS)),
    MISC("Misc", "Other HUD indicators", EnumSet.of(HudModule.FPS, HudModule.CPS, HudModule.CLOCK)),
    SETTINGS("Settings", "Minecraft settings", EnumSet.noneOf(HudModule.class));

    final String label, description;
    private final EnumSet<HudModule> included;
    HudSection(String label, String description, EnumSet<HudModule> included) {
        this.label = label; this.description = description; this.included = included;
    }
    List<HudModule> search(String query) {
        List<HudModule> result = new ArrayList<HudModule>();
        for (HudModule module : HudModule.search(query)) if (included.contains(module)) result.add(module);
        return Collections.unmodifiableList(result);
    }
}
