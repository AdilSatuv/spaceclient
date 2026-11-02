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
    COMPASS("compass", "Compass", "Direction you are facing", "direction facing heading компас направление стороны света"),
    CLOCK("clock", "Clock", "Local time on this computer", "time local часы время"),
    SPEED("speed", "Speed", "Horizontal movement per tick", "movement velocity blocks скорость движение блоки"),
    HEALTH("health", "Health", "Current and maximum health", "hp hearts здоровье жизни сердца"),
    HUNGER("hunger", "Hunger", "Current food level", "food hunger сытость голод еда"),
    ARMOR("armor", "Armor", "Armor and held item durability", "armor status durability equipment броня прочность экипировка"),
    POTION_EFFECTS("effects", "Potion Effects", "Active potion effects", "potion status effects effects зелья эффекты"),
    MEMORY("memory", "Memory", "Java memory used and maximum", "memory ram память озу"),
    PLAYTIME("playtime", "Playtime", "Time in this world, excluding pauses", "session time timer время сессия"),
    ITEM_COUNTER("itemcounter", "Item Counter", "Held item total in your main inventory", "item count inventory предметы счетчик счётчик"),
    SPRINT_STATUS("sprintstatus", "ToggleSprint HUD", "Sprint toggle and current running state", "autosprint togglesprint sprint status бег спринт статус"),
    PING("ping", "Ping", "Server-reported latency in milliseconds", "ping latency network пинг задержка сеть"),
    BIOME("biome", "Biome", "The biome at your current position", "biome world биом мир"),
    PACK_DISPLAY("packs", "Pack Display", "Selected resource packs and server pack", "pack resource textures ресурспак текстуры пак"),
    DAY_COUNTER("daycounter", "Day Counter", "Current world day, starting at 1", "day counter time world дни день счётчик счетчик мир"),
    LIGHT_LEVEL("lightlevel", "Light Level", "Sky and block light at your feet", "light sky block lighting свет освещение небо блок"),
    EXPERIENCE("experience", "Experience", "Your level and progress to the next level", "xp exp level progress опыт уровень прогресс"),
    ARROW_COUNTER("arrows", "Arrow Counter", "Arrows in your main inventory and hotbar", "arrows bow ammunition стрелы лук боезапас");

    private final String id;
    private final String title;
    private final String description;
    private final String searchText;
    private static final HudModule[] BROWSER_ORDER = {
        FPS, COORDINATES, KEYSTROKES, CPS, COMPASS, ARMOR,
        CLOCK, SPEED, HEALTH, HUNGER, POTION_EFFECTS, MEMORY, PLAYTIME, ITEM_COUNTER, SPRINT_STATUS,
        PING, BIOME, PACK_DISPLAY, DAY_COUNTER, LIGHT_LEVEL, EXPERIENCE, ARROW_COUNTER
    };

    HudModule(String id, String title, String description, String aliases) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.searchText = (id + " " + title + " " + description + " " + aliases).toLowerCase(Locale.ROOT);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDisplayDescription() {
        switch (this) {
            case FPS: return "Shows your current frames per second in-game.";
            case COORDINATES: return "Displays your current X, Y, Z position in the world.";
            case KEYSTROKES: return "Visualizes your keyboard inputs in real time.";
            case CPS: return "Shows your current clicks per second.";
            case COMPASS: return "Displays your current direction and rotation.";
            case ARMOR: return "Shows your current armor durability.";
            default: return description;
        }
    }

    public boolean isEnabled(HudConfig config) {
        switch (this) {
            case FPS: return config.active().fps;
            case COORDINATES: return config.active().coordinates;
            case KEYSTROKES: return config.active().keystrokes;
            case CPS: return config.active().cps;
            case COMPASS: return config.active().compass;
            case CLOCK: return config.active().clock;
            case SPEED: return config.active().speed;
            case HEALTH: return config.active().health;
            case HUNGER: return config.active().hunger;
            case ARMOR: return config.active().armor;
            case POTION_EFFECTS: return config.active().effects;
            case MEMORY: return config.active().memory;
            case PLAYTIME: return config.active().playtime;
            case ITEM_COUNTER: return config.active().itemCounter;
            case SPRINT_STATUS: return config.active().sprintStatus;
            case PING: return config.active().ping;
            case BIOME: return config.active().biome;
            case PACK_DISPLAY: return config.active().packs;
            case DAY_COUNTER: return config.active().dayCounter;
            case LIGHT_LEVEL: return config.active().lightLevel;
            case EXPERIENCE: return config.active().experience;
            case ARROW_COUNTER: return config.active().arrows;
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
            case CLOCK: config.active().clock = enabled; break;
            case SPEED: config.active().speed = enabled; break;
            case HEALTH: config.active().health = enabled; break;
            case HUNGER: config.active().hunger = enabled; break;
            case ARMOR: config.active().armor = enabled; break;
            case POTION_EFFECTS: config.active().effects = enabled; break;
            case MEMORY: config.active().memory = enabled; break;
            case PLAYTIME: config.active().playtime = enabled; break;
            case ITEM_COUNTER: config.active().itemCounter = enabled; break;
            case SPRINT_STATUS: config.active().sprintStatus = enabled; break;
            case PING: config.active().ping = enabled; break;
            case BIOME: config.active().biome = enabled; break;
            case PACK_DISPLAY: config.active().packs = enabled; break;
            case DAY_COUNTER: config.active().dayCounter = enabled; break;
            case LIGHT_LEVEL: config.active().lightLevel = enabled; break;
            case EXPERIENCE: config.active().experience = enabled; break;
            case ARROW_COUNTER: config.active().arrows = enabled; break;
            default: throw new AssertionError(this);
        }
    }

    public static List<HudModule> search(String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        List<HudModule> result = new ArrayList<>();
        for (HudModule module : BROWSER_ORDER) {
            if (module.searchText.contains(normalized)) result.add(module);
        }
        return Collections.unmodifiableList(result);
    }
}
