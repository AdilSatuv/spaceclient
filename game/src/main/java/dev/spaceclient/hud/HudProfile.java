package dev.spaceclient.hud;

import java.util.EnumMap;

/** Independent HUD preset; deliberately unrelated to launch profiles. */
public final class HudProfile {
    public String name;
    public boolean enabled = true, fps = true, coordinates = true, keystrokes = true, cps = true;
    public boolean compass = false;
    public boolean clock = false, speed = false;
    public boolean health = false, hunger = false;
    public boolean armor = false, effects = false;
    private final EnumMap<HudModule, HudPlacement> placements = new EnumMap<>(HudModule.class);
    public HudProfile(String name) { this.name = validName(name); resetLayout(); }
    public static String validName(String value) {
        if (value == null || !value.equals(value.trim()) || value.isEmpty()
                || value.codePointCount(0, value.length()) > 32 || value.indexOf('\u00a7') >= 0
                || value.codePoints().anyMatch(cp -> Character.isISOControl(cp)
                    || Character.getType(cp) == Character.FORMAT || Character.getType(cp) == Character.SURROGATE
                    || cp == 0x2028 || cp == 0x2029))
            throw new IllegalArgumentException("HUD profile name must contain 1-32 plain characters");
        return value;
    }
    public HudPlacement placement(HudModule module) { return placements.get(module); }
    public void place(HudModule module, HudPlacement placement) {
        if (module == null || placement == null) throw new IllegalArgumentException("Missing HUD placement");
        placements.put(module, placement);
    }
    public void resetLayout() {
        for (HudModule module : HudModule.values()) placements.put(module, HudPlacement.defaults(module));
    }
    public HudProfile copy() {
        HudProfile copy = new HudProfile(name);
        copy.enabled = enabled; copy.fps = fps; copy.coordinates = coordinates;
        copy.keystrokes = keystrokes; copy.cps = cps; copy.compass = compass;
        copy.clock = clock; copy.speed = speed;
        copy.health = health; copy.hunger = hunger;
        copy.armor = armor; copy.effects = effects;
        copy.placements.putAll(placements); // Placements are immutable.
        return copy;
    }
}
