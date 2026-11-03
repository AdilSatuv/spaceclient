package dev.spaceclient;

import dev.spaceclient.hud.HudAppearance;
import dev.spaceclient.hud.EquipmentContent;
import dev.spaceclient.hud.HudContentSettings;
import dev.spaceclient.hud.HudModule;
import dev.spaceclient.hud.HudPlacement;
import dev.spaceclient.hud.HudProfile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class HudConfig {
    public static final int MAX_BYTES = 65536;
    private final HudProfile[] profiles = { new HudProfile("Profile 1"), new HudProfile("Profile 2") };
    private int activeProfile;
    private int migrationSourceVersion;

    public HudProfile active() { return profiles[activeProfile]; }
    public HudProfile profile(int index) {
        if (index < 0 || index >= profiles.length) throw new IllegalArgumentException("Unknown HUD profile");
        return profiles[index];
    }
    public int activeIndex() { return activeProfile; }
    public void select(int index) { profile(index); activeProfile = index; }
    public void replaceActive(HudProfile profile) { profiles[activeProfile] = profile.copy(); }
    public int migrationSourceVersion() { return migrationSourceVersion; }
    public HudConfig copy() { HudConfig copy = new HudConfig(); copy.restore(this); return copy; }
    public void restore(HudConfig source) {
        for (int i = 0; i < profiles.length; i++) profiles[i] = source.profiles[i].copy();
        activeProfile = source.activeProfile;
        migrationSourceVersion = source.migrationSourceVersion;
    }

    public static HudConfig load(Path file) throws IOException {
        HudConfig config = new HudConfig();
        if (!Files.exists(file)) return config;
        Properties properties = readProperties(file);
        String schema = properties.getProperty("schemaVersion", "1");
        if (!schema.matches("[1-9][0-9]*")) throw new InvalidConfigException("Invalid HUD schema version");
        if (!schema.matches("[1-9]|1[0-8]")) throw new UnsupportedSchemaException();
        int version = Integer.parseInt(schema);
        if (version != 18) config.migrationSourceVersion = version;
        if (schema.equals("1")) {
            config.profiles[0] = readProfile(properties, "", "Profile 1", version);
            config.profiles[1] = config.profiles[0].copy();
            config.profiles[1].name = "Profile 2";
        } else {
            try { config.select(Integer.parseInt(properties.getProperty("activeProfile", "0"))); }
            catch (IllegalArgumentException exception) { throw new InvalidConfigException("Invalid active HUD profile", exception); }
            for (int i = 0; i < config.profiles.length; i++)
                config.profiles[i] = readProfile(properties, "profile." + i + ".", "Profile " + (i + 1), version);
        }
        if (version >= 7) {
            // Modern schemas are written as complete snapshots; missing keys must not
            // silently reset layout or flags on a subsequent save.
            Properties required = new Properties();
            required.setProperty("schemaVersion", schema);
            required.setProperty("activeProfile", Integer.toString(config.activeProfile));
            for (int i = 0; i < config.profiles.length; i++)
                writeProfile(required, "profile." + i + ".", config.profiles[i]);
            if (schema.equals("7")) for (int i = 0; i < config.profiles.length; i++)
                removeContentFields(required, "profile." + i + ".");
            if (version < 9) for (int i = 0; i < config.profiles.length; i++)
                removeExtendedFields(required, "profile." + i + ".");
            if (version < 10) for (int i = 0; i < config.profiles.length; i++)
                removeSprintFields(required, "profile." + i + ".");
            if (version < 11) for (int i = 0; i < config.profiles.length; i++)
                removeContextFields(required, "profile." + i + ".");
            if (version < 12) for (int i = 0; i < config.profiles.length; i++)
                removeContextContentFields(required, "profile." + i + ".");
            if (version < 13) for (int i = 0; i < config.profiles.length; i++)
                removeWidgetFormats(required, "profile." + i + ".");
            if (version < 14) for (int i = 0; i < config.profiles.length; i++)
                removeEquipmentFormats(required, "profile." + i + ".");
            if (version < 15) for (int i = 0; i < config.profiles.length; i++)
                removeWorldFields(required,"profile."+i+".");
            if (version < 16) for (int i = 0; i < config.profiles.length; i++)
                removePlayerStatsFields(required,"profile."+i+".");
            if (version < 17) for (int i = 0; i < config.profiles.length; i++)
                removeInventoryFields(required,"profile."+i+".");
            if(version<18) for(int i=0;i<config.profiles.length;i++) removeExpansionFields(required,"profile."+i+".");
            if (!properties.stringPropertyNames().containsAll(required.stringPropertyNames()))
                throw new InvalidConfigException("Incomplete HUD schema " + schema + " configuration");
        }
        return config;
    }

    private static Properties readProperties(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (bytes.size() + count > MAX_BYTES) throw new InvalidConfigException("HUD config exceeds 64 KiB");
                bytes.write(buffer, 0, count);
            }
            try { properties.load(new ByteArrayInputStream(bytes.toByteArray())); }
            catch (IllegalArgumentException exception) { throw new InvalidConfigException("Malformed HUD properties", exception); }
        }
        return properties;
    }

    private static HudProfile readProfile(Properties properties, String prefix, String defaultName, int version) throws IOException {
        try {
            boolean appearanceFields = version >= 7, contentFields = version >= 8, extendedFields = version >= 9;
            HudProfile profile = new HudProfile(properties.getProperty(prefix + "name", defaultName));
            profile.enabled = readBoolean(properties, prefix + "enabled");
            profile.fps = readBoolean(properties, prefix + "fps");
            profile.coordinates = readBoolean(properties, prefix + "coordinates");
            profile.keystrokes = readBoolean(properties, prefix + "keystrokes");
            profile.cps = readBoolean(properties, prefix + "cps");
            profile.compass = readBoolean(properties, prefix + "compass", false);
            profile.clock = readBoolean(properties, prefix + "clock", false);
            profile.speed = readBoolean(properties, prefix + "speed", false);
            profile.health = readBoolean(properties, prefix + "health", false);
            profile.hunger = readBoolean(properties, prefix + "hunger", false);
            profile.armor = readBoolean(properties, prefix + "armor", false);
            profile.effects = readBoolean(properties, prefix + "effects", false);
            profile.memory = extendedFields && readBoolean(properties, prefix + "memory", false);
            profile.playtime = extendedFields && readBoolean(properties, prefix + "playtime", false);
            profile.itemCounter = extendedFields && readBoolean(properties, prefix + "itemcounter", false);
            profile.sprintStatus = version < 10 || readBoolean(properties, prefix + "sprintstatus", true);
            profile.ping = version >= 11 && readBoolean(properties, prefix + "ping", false);
            profile.biome = version >= 11 && readBoolean(properties, prefix + "biome", false);
            profile.packs = version >= 11 && readBoolean(properties, prefix + "packs", false);
            profile.dayCounter=version>=15&&readBoolean(properties,prefix+"daycounter",false);
            profile.lightLevel=version>=15&&readBoolean(properties,prefix+"lightlevel",false);
            profile.experience=version>=16&&readBoolean(properties,prefix+"experience",false);
            profile.arrows=version>=16&&readBoolean(properties,prefix+"arrows",false);
            profile.combo=version>=18&&readBoolean(properties,prefix+"combo",false);
            profile.reach=version>=18&&readBoolean(properties,prefix+"reach",false);
            profile.saturation=version>=18&&readBoolean(properties,prefix+"saturation",false);
            profile.inventory=version>=17&&readBoolean(properties,prefix+"inventory",false);
            for (HudModule module : HudModule.values()) {
                HudPlacement defaults = HudPlacement.defaults(module);
                String key = prefix + "layout." + module.getId() + ".";
                profile.place(module, new HudPlacement(
                    HudPlacement.Horizontal.valueOf(properties.getProperty(key + "horizontal", defaults.horizontal.name())),
                    HudPlacement.Vertical.valueOf(properties.getProperty(key + "vertical", defaults.vertical.name())),
                    Double.parseDouble(properties.getProperty(key + "x", Double.toString(defaults.x))),
                    Double.parseDouble(properties.getProperty(key + "y", Double.toString(defaults.y))),
                    Double.parseDouble(properties.getProperty(key + "scale", "1.0"))));
                if (appearanceFields && (extendedFields || module.ordinal() < HudModule.MEMORY.ordinal())
                        && (version >= 10 || module != HudModule.SPRINT_STATUS)
                        && (version >= 11 || module.ordinal() < HudModule.PING.ordinal())
                        && (version >= 15 || module.ordinal() < HudModule.DAY_COUNTER.ordinal())
                        && (version >= 16 || module.ordinal() < HudModule.EXPERIENCE.ordinal())
                        && (version >= 17 || module.ordinal() < HudModule.INVENTORY.ordinal())
                        && (version >= 18 || module.ordinal() < HudModule.COMBO_COUNTER.ordinal())) {
                    String appearanceKey = prefix + "appearance." + module.getId() + ".";
                    String color = properties.getProperty(appearanceKey + "color");
                    String backdrop = properties.getProperty(appearanceKey + "backdrop");
                    String shadow = properties.getProperty(appearanceKey + "shadow");
                    if (color == null || backdrop == null || shadow == null)
                        throw new InvalidConfigException("Missing HUD appearance: " + module.getId());
                    profile.setAppearance(module, new HudAppearance(HudAppearance.Color.valueOf(color),
                        HudAppearance.Backdrop.valueOf(backdrop), readBoolean(properties, appearanceKey + "shadow")));
                }
            }
            if (contentFields) {
                String precision = properties.getProperty(prefix + "content.coordinates.precision");
                String format = properties.getProperty(prefix + "content.clock.format");
                String seconds = properties.getProperty(prefix + "content.clock.seconds");
                String fpsLabel = properties.getProperty(prefix + "content.fps.label");
                String cpsMode = properties.getProperty(prefix + "content.cps.mode");
                if (precision == null || format == null || seconds == null || fpsLabel == null || cpsMode == null)
                    throw new InvalidConfigException("Missing HUD content settings");
                profile.setContent(new HudContentSettings(Integer.parseInt(precision),
                    HudContentSettings.ClockFormat.valueOf(format), readBoolean(properties, prefix + "content.clock.seconds"),
                    readBoolean(properties, prefix + "content.fps.label"), HudContentSettings.CpsMode.valueOf(cpsMode),
                    version < 11 || readBoolean(properties,prefix + "content.keystrokes.jump"),
                    version < 11 || readBoolean(properties,prefix + "content.keystrokes.mouse"),
                    version < 12 || readBoolean(properties,prefix + "content.ping.label"),
                    version < 12 || readBoolean(properties,prefix + "content.biome.label"),
                    version < 12 || readBoolean(properties,prefix + "content.packs.label"),
                    version < 12 ? 2 : Integer.parseInt(properties.getProperty(prefix + "content.packs.names", "")),
                    version < 13 ? HudContentSettings.KeyLayout.LINE : HudContentSettings.KeyLayout.valueOf(properties.getProperty(prefix + "content.keystrokes.layout", "")),
                    version < 13 ? HudContentSettings.SprintDisplay.FULL : HudContentSettings.SprintDisplay.valueOf(properties.getProperty(prefix + "content.sprint.display", "")),
                    version < 14 ? EquipmentContent.defaults() : new EquipmentContent(
                        EquipmentContent.Style.valueOf(properties.getProperty(prefix+"content.armor.style","")),
                        EquipmentContent.Durability.valueOf(properties.getProperty(prefix+"content.armor.durability","")),
                        readBoolean(properties,prefix+"content.armor.hand"),
                        EquipmentContent.Style.valueOf(properties.getProperty(prefix+"content.effects.style","")),
                        Integer.parseInt(properties.getProperty(prefix+"content.effects.limit","")),
                        readBoolean(properties,prefix+"content.effects.time")),
                    version >= 17 && readBoolean(properties,prefix+"content.inventory.hotbar"),
                    version < 17 || readBoolean(properties,prefix+"content.inventory.overlays")));
            }
            return profile;
        } catch (IllegalArgumentException exception) { throw new InvalidConfigException("Invalid HUD profile", exception); }
    }

    private static boolean readBoolean(Properties properties, String key) throws IOException {
        return readBoolean(properties, key, true);
    }
    private static boolean readBoolean(Properties properties, String key, boolean defaultValue) throws IOException {
        String value = properties.getProperty(key, Boolean.toString(defaultValue));
        if (!value.equals("true") && !value.equals("false")) throw new InvalidConfigException("Invalid HUD setting: " + key);
        return Boolean.parseBoolean(value);
    }

    private static void writeProfile(Properties properties, String prefix, HudProfile profile) throws IOException {
        try { HudProfile.validName(profile.name); }
        catch (IllegalArgumentException exception) { throw new InvalidConfigException("Invalid HUD profile name", exception); }
        properties.setProperty(prefix + "name", profile.name);
        properties.setProperty(prefix + "enabled", Boolean.toString(profile.enabled));
        properties.setProperty(prefix + "fps", Boolean.toString(profile.fps));
        properties.setProperty(prefix + "coordinates", Boolean.toString(profile.coordinates));
        properties.setProperty(prefix + "keystrokes", Boolean.toString(profile.keystrokes));
        properties.setProperty(prefix + "cps", Boolean.toString(profile.cps));
        properties.setProperty(prefix + "compass", Boolean.toString(profile.compass));
        properties.setProperty(prefix + "clock", Boolean.toString(profile.clock));
        properties.setProperty(prefix + "speed", Boolean.toString(profile.speed));
        properties.setProperty(prefix + "health", Boolean.toString(profile.health));
        properties.setProperty(prefix + "hunger", Boolean.toString(profile.hunger));
        properties.setProperty(prefix + "armor", Boolean.toString(profile.armor));
        properties.setProperty(prefix + "effects", Boolean.toString(profile.effects));
        properties.setProperty(prefix + "memory", Boolean.toString(profile.memory));
        properties.setProperty(prefix + "playtime", Boolean.toString(profile.playtime));
        properties.setProperty(prefix + "itemcounter", Boolean.toString(profile.itemCounter));
        properties.setProperty(prefix + "sprintstatus", Boolean.toString(profile.sprintStatus));
        properties.setProperty(prefix + "ping", Boolean.toString(profile.ping));
        properties.setProperty(prefix + "biome", Boolean.toString(profile.biome));
        properties.setProperty(prefix + "packs", Boolean.toString(profile.packs));
        properties.setProperty(prefix+"daycounter",Boolean.toString(profile.dayCounter));
        properties.setProperty(prefix+"lightlevel",Boolean.toString(profile.lightLevel));
        properties.setProperty(prefix+"experience",Boolean.toString(profile.experience));
        properties.setProperty(prefix+"arrows",Boolean.toString(profile.arrows));
        properties.setProperty(prefix+"combo",Boolean.toString(profile.combo));
        properties.setProperty(prefix+"reach",Boolean.toString(profile.reach));
        properties.setProperty(prefix+"saturation",Boolean.toString(profile.saturation));
        properties.setProperty(prefix+"inventory",Boolean.toString(profile.inventory));
        HudContentSettings content = profile.content();
        properties.setProperty(prefix + "content.coordinates.precision", Integer.toString(content.coordinatesPrecision));
        properties.setProperty(prefix + "content.clock.format", content.clockFormat.name());
        properties.setProperty(prefix + "content.clock.seconds", Boolean.toString(content.clockSeconds));
        properties.setProperty(prefix + "content.fps.label", Boolean.toString(content.fpsLabel));
        properties.setProperty(prefix + "content.cps.mode", content.cpsMode.name());
        properties.setProperty(prefix + "content.keystrokes.jump", Boolean.toString(content.keystrokesJump));
        properties.setProperty(prefix + "content.keystrokes.mouse", Boolean.toString(content.keystrokesMouse));
        properties.setProperty(prefix + "content.ping.label", Boolean.toString(content.pingLabel));
        properties.setProperty(prefix + "content.biome.label", Boolean.toString(content.biomeLabel));
        properties.setProperty(prefix + "content.packs.label", Boolean.toString(content.packLabel));
        properties.setProperty(prefix + "content.packs.names", Integer.toString(content.packNames));
        properties.setProperty(prefix + "content.keystrokes.layout", content.keyLayout.name());
        properties.setProperty(prefix + "content.sprint.display", content.sprintDisplay.name());
        EquipmentContent equipment=content.equipment;
        properties.setProperty(prefix+"content.armor.style",equipment.armorStyle.name());
        properties.setProperty(prefix+"content.armor.durability",equipment.durability.name());
        properties.setProperty(prefix+"content.armor.hand",Boolean.toString(equipment.hand));
        properties.setProperty(prefix+"content.effects.style",equipment.effectStyle.name());
        properties.setProperty(prefix+"content.effects.limit",Integer.toString(equipment.maxEffects));
        properties.setProperty(prefix+"content.effects.time",Boolean.toString(equipment.effectTime));
        properties.setProperty(prefix+"content.inventory.hotbar",Boolean.toString(content.inventoryHotbar));
        properties.setProperty(prefix+"content.inventory.overlays",Boolean.toString(content.inventoryOverlays));
        for (HudModule module : HudModule.values()) {
            HudPlacement placement = profile.placement(module);
            String key = prefix + "layout." + module.getId() + ".";
            properties.setProperty(key + "horizontal", placement.horizontal.name());
            properties.setProperty(key + "vertical", placement.vertical.name());
            properties.setProperty(key + "x", Double.toString(placement.x));
            properties.setProperty(key + "y", Double.toString(placement.y));
            properties.setProperty(key + "scale", Double.toString(placement.scale));
            HudAppearance appearance = profile.appearance(module);
            String appearanceKey = prefix + "appearance." + module.getId() + ".";
            properties.setProperty(appearanceKey + "color", appearance.color.name());
            properties.setProperty(appearanceKey + "backdrop", appearance.backdrop.name());
            properties.setProperty(appearanceKey + "shadow", Boolean.toString(appearance.shadow));
        }
    }

    public void save(Path file) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("schemaVersion", "18");
        properties.setProperty("activeProfile", Integer.toString(activeProfile));
        for (int i = 0; i < profiles.length; i++) writeProfile(properties, "profile." + i + ".", profiles[i]);
        writeAtomic(file, properties);
    }

    public static void exportProfile(Path file, HudProfile profile) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("format", "spaceclient-hud-profile");
        properties.setProperty("schemaVersion", "13");
        writeProfile(properties, "", profile);
        writeAtomic(file, properties);
    }

    public static HudProfile importProfile(Path file) throws IOException {
        Properties properties = readProperties(file);
        String version = properties.getProperty("schemaVersion");
        if (!"spaceclient-hud-profile".equals(properties.getProperty("format"))
                || version == null || !version.matches("[1-9]|1[0-3]"))
            throw new InvalidConfigException("Unsupported HUD profile export");
        HudProfile profile = readProfile(properties, "", "Imported", Integer.parseInt(version) + 5);
        Properties expected = new Properties();
        expected.setProperty("format", "spaceclient-hud-profile");
        expected.setProperty("schemaVersion", version);
        writeProfile(expected, "", profile);
        if (Integer.parseInt(version) < 3) removeContentFields(expected, "");
        if (Integer.parseInt(version) < 4) removeExtendedFields(expected, "");
        if (Integer.parseInt(version) < 5) removeSprintFields(expected, "");
        if (Integer.parseInt(version) < 6) removeContextFields(expected, "");
        if (Integer.parseInt(version) < 7) removeContextContentFields(expected, "");
        if (Integer.parseInt(version) < 8) removeWidgetFormats(expected, "");
        if (Integer.parseInt(version) < 9) removeEquipmentFormats(expected, "");
        if (Integer.parseInt(version) < 10) removeWorldFields(expected, "");
        if (Integer.parseInt(version) < 11) removePlayerStatsFields(expected, "");
        if(Integer.parseInt(version)<13)removeExpansionFields(expected, "");
        if (Integer.parseInt(version) < 12) removeInventoryFields(expected, "");
        if (!"1".equals(version)) {
            if (!properties.stringPropertyNames().equals(expected.stringPropertyNames()))
                throw new InvalidConfigException("Incomplete or unknown HUD profile fields");
            return profile;
        }
        // Version 1 did not contain appearance fields. Accept only complete historical shapes.
        for (HudModule module : HudModule.values()) removeAppearanceFields(expected, module.getId());
        Properties vitalsEra = new Properties();
        vitalsEra.putAll(expected);
        removeModuleFields(vitalsEra, "armor");
        removeModuleFields(vitalsEra, "effects");
        Properties metricsEra = new Properties();
        metricsEra.putAll(vitalsEra);
        removeModuleFields(metricsEra, "health");
        removeModuleFields(metricsEra, "hunger");
        Properties compassEra = new Properties();
        compassEra.putAll(metricsEra);
        removeModuleFields(compassEra, "clock");
        removeModuleFields(compassEra, "speed");
        Properties legacy = new Properties();
        legacy.putAll(compassEra);
        removeModuleFields(legacy, "compass");
        if (!properties.stringPropertyNames().equals(expected.stringPropertyNames())
                && !properties.stringPropertyNames().equals(vitalsEra.stringPropertyNames())
                && !properties.stringPropertyNames().equals(metricsEra.stringPropertyNames())
                && !properties.stringPropertyNames().equals(compassEra.stringPropertyNames())
                && !properties.stringPropertyNames().equals(legacy.stringPropertyNames()))
            throw new InvalidConfigException("Incomplete or unknown HUD profile fields");
        return profile;
    }

    private static void removeContentFields(Properties properties, String prefix) {
        removeContextContentFields(properties, prefix);
        removeWidgetFormats(properties, prefix);
        removeEquipmentFormats(properties,prefix);
        properties.remove(prefix + "content.coordinates.precision");
        properties.remove(prefix + "content.clock.format");
        properties.remove(prefix + "content.clock.seconds");
        properties.remove(prefix + "content.fps.label");
        properties.remove(prefix + "content.cps.mode");
        properties.remove(prefix + "content.keystrokes.jump");
        properties.remove(prefix + "content.keystrokes.mouse");
    }
    private static void removeExpansionFields(Properties p,String prefix){for(String id:new String[]{"combo","reach","saturation"}){p.remove(prefix+id);for(String k:new String[]{"horizontal","vertical","x","y","scale"})p.remove(prefix+"layout."+id+"."+k);for(String k:new String[]{"color","backdrop","shadow"})p.remove(prefix+"appearance."+id+"."+k);}}
    private static void removeInventoryFields(Properties properties,String prefix) {
        properties.remove(prefix+"inventory");
        properties.remove(prefix+"content.inventory.hotbar");properties.remove(prefix+"content.inventory.overlays");
        for(String key:new String[]{"horizontal","vertical","x","y","scale"})properties.remove(prefix+"layout.inventory."+key);
        for(String key:new String[]{"color","backdrop","shadow"})properties.remove(prefix+"appearance.inventory."+key);
    }
    private static void removePlayerStatsFields(Properties properties,String prefix) {
        for(String id:new String[]{"experience","arrows"}) {
            properties.remove(prefix+id);
            for(String key:new String[]{"horizontal","vertical","x","y","scale"})properties.remove(prefix+"layout."+id+"."+key);
            for(String key:new String[]{"color","backdrop","shadow"})properties.remove(prefix+"appearance."+id+"."+key);
        }
    }
    private static void removeWorldFields(Properties properties,String prefix) {
        for(String id:new String[]{"daycounter","lightlevel"}) {
            properties.remove(prefix+id);
            for(String key:new String[]{"horizontal","vertical","x","y","scale"})properties.remove(prefix+"layout."+id+"."+key);
            for(String key:new String[]{"color","backdrop","shadow"})properties.remove(prefix+"appearance."+id+"."+key);
        }
    }
    private static void removeEquipmentFormats(Properties properties,String prefix) {
        for(String key:new String[]{"armor.style","armor.durability","armor.hand","effects.style","effects.limit","effects.time"})properties.remove(prefix+"content."+key);
    }
    private static void removeWidgetFormats(Properties properties, String prefix) {
        properties.remove(prefix + "content.keystrokes.layout"); properties.remove(prefix + "content.sprint.display");
    }
    private static void removeContextContentFields(Properties properties, String prefix) {
        for (String key : new String[]{"ping.label","biome.label","packs.label","packs.names"}) properties.remove(prefix + "content." + key);
    }
    private static void removeExtendedFields(Properties properties, String prefix) {
        for (String module : new String[] {"memory", "playtime", "itemcounter"}) {
            properties.remove(prefix + module);
            for (String field : new String[] {"horizontal", "vertical", "x", "y", "scale"})
                properties.remove(prefix + "layout." + module + "." + field);
            for (String field : new String[] {"color", "backdrop", "shadow"})
                properties.remove(prefix + "appearance." + module + "." + field);
        }
    }
    private static void removeSprintFields(Properties properties, String prefix) {
        properties.remove(prefix + "sprintstatus");
        for (String field : new String[] {"horizontal", "vertical", "x", "y", "scale"})
            properties.remove(prefix + "layout.sprintstatus." + field);
        for (String field : new String[] {"color", "backdrop", "shadow"})
            properties.remove(prefix + "appearance.sprintstatus." + field);
    }
    private static void removeContextFields(Properties properties,String prefix) {
        for(String id:new String[]{"ping","biome","packs"}) {
            properties.remove(prefix+id);
            for(String field:new String[]{"horizontal","vertical","x","y","scale"})properties.remove(prefix+"layout."+id+"."+field);
            for(String field:new String[]{"color","backdrop","shadow"})properties.remove(prefix+"appearance."+id+"."+field);
        }
        properties.remove(prefix+"content.keystrokes.jump");properties.remove(prefix+"content.keystrokes.mouse");
    }

    private static void removeAppearanceFields(Properties properties, String module) {
        for (String field : new String[] { "color", "backdrop", "shadow" })
            properties.remove("appearance." + module + "." + field);
    }

    private static void removeModuleFields(Properties properties, String module) {
        properties.remove(module);
        for (String field : new String[] { "horizontal", "vertical", "x", "y", "scale" })
            properties.remove("layout." + module + "." + field);
    }

    private static void writeAtomic(Path file, Properties properties) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "space-hud-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) { properties.store(output, "Space Client HUD"); }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    public static final class InvalidConfigException extends IOException {
        public InvalidConfigException(String message) { super(message); }
        public InvalidConfigException(String message, Throwable cause) { super(message, cause); }
    }
    public static final class UnsupportedSchemaException extends IOException {
        public UnsupportedSchemaException() { super("Unsupported HUD schema version"); }
    }
}
