package dev.spaceclient.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import dev.spaceclient.mods.GameplayModule;

/** Navigation filters existing HUD modules; Settings opens the native options screen. */
enum HudSection {
    HOME("Home", "Your Space HUD modules", EnumSet.allOf(HudModule.class)),
    COMBAT("Combat", "Combat-related HUD indicators", EnumSet.of(HudModule.CPS, HudModule.KEYSTROKES, HudModule.HEALTH, HudModule.ARMOR, HudModule.POTION_EFFECTS, HudModule.PING)),
    VISUALS("Visuals", "Visual information on your HUD", EnumSet.of(HudModule.FPS, HudModule.COORDINATES, HudModule.COMPASS, HudModule.CLOCK, HudModule.POTION_EFFECTS, HudModule.PACK_DISPLAY)),
    HUD("HUD", "Customize your in-game overlay", EnumSet.allOf(HudModule.class)),
    PLAYER("Player", "Player HUD indicators", EnumSet.of(HudModule.KEYSTROKES, HudModule.SPEED, HudModule.HEALTH, HudModule.HUNGER, HudModule.ARMOR, HudModule.POTION_EFFECTS, HudModule.ITEM_COUNTER, HudModule.SPRINT_STATUS)),
    WORLD("World", "Position, world days and lighting", EnumSet.of(HudModule.COORDINATES, HudModule.COMPASS, HudModule.BIOME, HudModule.DAY_COUNTER, HudModule.LIGHT_LEVEL)),
    MISC("Misc", "Other HUD indicators", EnumSet.of(HudModule.FPS, HudModule.CPS, HudModule.CLOCK, HudModule.MEMORY, HudModule.PLAYTIME, HudModule.PING, HudModule.PACK_DISPLAY)),
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
    List<CatalogModule> catalog(String query) {
        List<CatalogModule> result = new ArrayList<CatalogModule>();
        for (GameplayModule module : GameplayModule.values()) if (includes(module) && module.matches(query)) result.add(CatalogModule.game(module));
        for (HudModule module : search(query)) result.add(CatalogModule.hud(module));
        return Collections.unmodifiableList(result);
    }
    private boolean includes(GameplayModule module) {
        if (this == HOME) return true;
        if (this == PLAYER) return module == GameplayModule.TOGGLE_SPRINT || module == GameplayModule.FREE_LOOK;
        if (this == VISUALS) return module != GameplayModule.TOGGLE_SPRINT;
        return this == COMBAT && module == GameplayModule.CROSSHAIR;
    }
}
